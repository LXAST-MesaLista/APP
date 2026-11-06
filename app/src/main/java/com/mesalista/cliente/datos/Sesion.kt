package com.mesalista.cliente.datos

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Cliente con la sesión abierta. */
data class Usuario(val id: Int, val nombre: String, val email: String, val telefono: String) {
    companion object {
        fun de(j: JSONObject) = Usuario(j.optInt("id"), j.optString("name"), j.optString("email"), j.optString("phone"))
    }

    fun json(): JSONObject = JSONObject().put("id", id).put("name", nombre).put("email", email).put("phone", telefono)
}

/**
 * Sesión del cliente. El token que da el servidor al entrar se guarda cifrado
 * con AES-GCM y una clave del Android Keystore, que no sale de este móvil
 * (nunca en texto plano ni en los registros). La interfaz observa [usuario]:
 * con null muestra «Entrar».
 */
class Sesion(contexto: Context) {

    private val prefs = contexto.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    /** Motivo de la última salida automática, para avisar en «Entrar» (se lee una vez). */
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    @Volatile
    var token: String? = null
        private set

    init {
        runCatching {
            val t = prefs.getString("token", null)?.let(::descifrar)
            val u = prefs.getString("usuario", null)?.let(::descifrar)?.let { Usuario.de(JSONObject(it)) }
            if (t != null && u != null) {
                token = t
                _usuario.value = u
            }
        }.onFailure { borrar() }   // clave perdida (p. ej. tras restaurar el móvil): a entrar de nuevo
    }

    /** Respuesta de /auth/login o /auth/register: {"user": {…}, "session": {"token": …}}. */
    fun entrar(respuesta: JSONObject) {
        val t = respuesta.getJSONObject("session").getString("token")
        val u = Usuario.de(respuesta.getJSONObject("user"))
        prefs.edit().putString("token", cifrar(t)).putString("usuario", cifrar(u.json().toString())).apply()
        token = t
        _aviso.value = null
        _usuario.value = u
    }

    fun actualizar(u: Usuario) {
        if (token == null) return
        prefs.edit().putString("usuario", cifrar(u.json().toString())).apply()
        _usuario.value = u
    }

    /** Cerrar la sesión en el móvil (la del servidor la cierra /auth/logout). */
    fun salir(aviso: String? = null) {
        borrar()
        _aviso.value = aviso
    }

    /** El servidor ha respondido 401: el token ya no vale. */
    fun caducada() = salir("La sesión ha caducado. Vuelve a entrar.")

    fun avisoLeido() {
        _aviso.value = null
    }

    private fun borrar() {
        prefs.edit().clear().apply()
        token = null
        _usuario.value = null
    }

    // --- Cifrado con el Android Keystore ----------------------------------

    private fun clave(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun cifrar(texto: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, clave()) }
        val cifrado = c.doFinal(texto.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(c.iv + cifrado, Base64.NO_WRAP)
    }

    private fun descifrar(guardado: String): String {
        val bytes = Base64.decode(guardado, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(128, bytes, 0, 12))
        return String(c.doFinal(bytes, 12, bytes.size - 12), Charsets.UTF_8)
    }

    private companion object {
        const val ALIAS = "mesalista-sesion"
    }
}
