package com.mesalista.cliente.datos

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Error de la API: el servidor siempre responde
 * {"error": {"code": "…", "message": "…", "field": "…"}} con el mensaje ya en español.
 * status 0 = sin conexión.
 */
class ApiError(
    val status: Int,
    val codigo: String?,
    override val message: String,
    val campo: String?,
    val datos: JSONObject = JSONObject(),
) : Exception(message) {
    companion object {
        /** Nombre en pantalla de los campos que el servidor cita como «phone»… */
        private val ETIQUETAS = mapOf(
            "name" to "Nombre", "email" to "Correo electrónico", "phone" to "Teléfono",
            "password" to "Contraseña", "party" to "Comensales", "date" to "Día", "time" to "Hora",
            "note" to "Nota", "code" to "Código de reserva",
        )
    }

    /** El mensaje con el nombre del campo en español («El campo «Teléfono» debe…»). */
    val texto: String
        get() = campo?.let { c -> ETIQUETAS[c]?.let { message.replace("«$c»", "«$it»") } } ?: message
}

/**
 * Llamadas a la API del servidor de MesaLista (/api/v1). Todas en JSON y con
 * el token de la sesión en «Authorization: Bearer». Si el servidor responde
 * 401 a una petición con token, la sesión ha caducado: se cierra en el móvil
 * y la app vuelve a «Entrar».
 */
class Api(private val base: String, private val sesion: Sesion) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = "application/json; charset=utf-8".toMediaType()

    suspend fun get(ruta: String): JSONObject = llamar("GET", ruta, null)
    suspend fun post(ruta: String, cuerpo: JSONObject = JSONObject()): JSONObject = llamar("POST", ruta, cuerpo)
    suspend fun patch(ruta: String, cuerpo: JSONObject): JSONObject = llamar("PATCH", ruta, cuerpo)
    suspend fun delete(ruta: String, cuerpo: JSONObject? = null): JSONObject = llamar("DELETE", ruta, cuerpo)

    suspend fun llamar(metodo: String, ruta: String, cuerpo: JSONObject?): JSONObject = withContext(Dispatchers.IO) {
        val token = sesion.token
        val peticion = Request.Builder()
            .url(base + ruta)
            .header("Accept", "application/json")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .method(metodo, cuerpo?.toString()?.toRequestBody(json))
            .build()
        val respuesta = try {
            http.newCall(peticion).execute()
        } catch (e: IOException) {
            throw ApiError(0, "network", "No se puede conectar con el servidor. Comprueba la conexión e inténtalo de nuevo.", null)
        }
        respuesta.use { res ->
            val texto = res.body?.string().orEmpty()
            val datos = runCatching { JSONObject(texto) }.getOrDefault(JSONObject())
            if (res.isSuccessful) {
                return@withContext datos
            }
            if (res.code == 401 && token != null) {
                sesion.caducada()
            }
            val error = datos.optJSONObject("error") ?: JSONObject()
            throw ApiError(
                status = res.code,
                codigo = error.optString("code").ifEmpty { null },
                message = error.optString("message").ifEmpty { mensajeGenerico(res.code) },
                campo = error.optString("field").ifEmpty { null },
                datos = error,
            )
        }
    }

    private fun mensajeGenerico(status: Int) = when (status) {
        429 -> "Demasiados intentos. Espera unos minutos y vuelve a intentarlo."
        in 500..599 -> "El servidor no está disponible ahora mismo. Inténtalo más tarde."
        else -> "Ha habido un problema. Inténtalo de nuevo."
    }
}
