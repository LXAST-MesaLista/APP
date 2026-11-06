package com.mesalista.cliente.datos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.mesalista.cliente.App
import com.mesalista.cliente.BuildConfig
import com.mesalista.cliente.MainActivity
import com.mesalista.cliente.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Avisos push con Firebase Cloud Messaging (FCM). Firebase solo entrega los
 * avisos: los decide y los envía el servidor de MesaLista cuando una reserva
 * se confirma, cambia o se cancela.
 *
 *  - Con sesión, el móvil se registra en el servidor (POST /me/devices) al
 *    abrir la app y cada vez que FCM le da un token nuevo.
 *  - Al cerrar sesión se da de baja (DELETE /me/devices) y, salga como salga
 *    (también si la sesión caduca), se anula el token: sin sesión el móvil no
 *    recibe avisos de nadie.
 *  - Los avisos solo traen la fecha, la hora, los comensales y el número de
 *    reserva; nunca el QR, las opciones ni la nota.
 *
 * Sin app/google-services.json (BuildConfig.CON_FIREBASE = false) no se hace nada.
 */
object Avisos {
    const val CANAL = "reservas"

    /** Tipos de aviso que envía el servidor (data.type). */
    val TIPOS = setOf(
        "confirmed", "modified", "cancelled_holder", "cancelled_restaurant",
        "group_accepted", "group_rejected", "payment_expired",
    )

    /** Ha llegado un aviso: las pantallas de reservas abiertas se recargan. */
    val cambios = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val activo: Boolean get() = BuildConfig.CON_FIREBASE

    /** Canal «Reservas» (Android 8+): el usuario puede silenciarlo en los ajustes del móvil. */
    fun crearCanal(contexto: Context) {
        val canal = NotificationChannel(CANAL, "Reservas", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Confirmaciones, cambios y cancelaciones de tus reservas"
        }
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /** ¿Puede la app mostrar avisos? (permiso en Android 13+ y canal sin silenciar). */
    fun permitidos(contexto: Context): Boolean {
        val gestor = NotificationManagerCompat.from(contexto)
        if (!gestor.areNotificationsEnabled()) return false
        val canal = gestor.getNotificationChannel(CANAL)
        return canal == null || canal.importance != NotificationManager.IMPORTANCE_NONE
    }

    /** Ajustes de avisos de la app en el móvil (para activarlos si se denegaron). */
    fun abrirAjustes(contexto: Context) {
        contexto.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, contexto.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** El permiso de Android 13+ se pide una sola vez; después, desde «Mi cuenta». */
    fun permisoPedido(contexto: Context): Boolean =
        contexto.getSharedPreferences("avisos", Context.MODE_PRIVATE).getBoolean("permiso_pedido", false)

    fun marcarPermisoPedido(contexto: Context) {
        contexto.getSharedPreferences("avisos", Context.MODE_PRIVATE).edit().putBoolean("permiso_pedido", true).apply()
    }

    /** Registra este móvil en el servidor. Si falla (sin conexión), se repite al volver a abrir la app. */
    suspend fun registrar(token: String? = null) {
        if (!activo || App.sesion.token == null) return
        runCatching {
            val t = token ?: FirebaseMessaging.getInstance().token.await()
            App.api.post("/me/devices", JSONObject().put("token", t).put("platform", "android"))
        }
    }

    /** Da de baja este móvil antes de cerrar la sesión (con la sesión aún abierta). */
    suspend fun darDeBaja() {
        if (!activo || App.sesion.token == null) return
        runCatching {
            val t = FirebaseMessaging.getInstance().token.await()
            App.api.delete("/me/devices", JSONObject().put("token", t))
        }
    }

    /** Sin sesión: se anula el token. El servidor lo olvida al ver que ya no vale; al entrar, FCM da otro. */
    suspend fun olvidarToken() {
        if (!activo) return
        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
    }

    /**
     * Muestra un aviso que ha llegado con la app abierta (con la app cerrada lo
     * muestra el propio sistema). Al tocarlo se abre la reserva.
     */
    fun mostrar(contexto: Context, titulo: String, texto: String, datos: Map<String, String>) {
        if (!permitidos(contexto)) return
        val tipo = datos["type"]?.takeIf { it in TIPOS }
        val reserva = datos["reservation_id"]?.takeIf { it.length <= 10 && it.all(Char::isDigit) }
        val abrir = Intent(contexto, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            tipo?.let { putExtra("type", it) }
            reserva?.let { putExtra("reservation_id", it) }
        }
        // Un aviso por reserva: el más reciente sustituye al anterior.
        val id = reserva?.toIntOrNull() ?: (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
        val alTocar = PendingIntent.getActivity(
            contexto, id, abrir, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val aviso = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ic_aviso)
            .setColor(ContextCompat.getColor(contexto, R.color.bar))
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(alTocar)
            .build()
        try {
            NotificationManagerCompat.from(contexto).notify("reserva", id, aviso)
        } catch (e: SecurityException) {
            // Permiso retirado justo ahora: no se muestra.
        }
    }
}
