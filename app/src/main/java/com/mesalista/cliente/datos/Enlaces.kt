package com.mesalista.cliente.datos

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Enlaces que abren la app en una reserva:
 *  - la vuelta de PayPal: mesalista://reserva/{id}?pago=ok|cancelado|caducado|error
 *    (el servidor redirige ahí al terminar el pago, RF-44);
 *  - al tocar un aviso push: type y reservation_id en los datos del aviso
 *    (los pone el sistema, o Avisos.mostrar con la app abierta).
 * Se guarda hasta que hay sesión y las pantallas del cliente lo atienden.
 */
object Enlaces {
    data class Destino(val reservaId: Int?, val pago: String?, val tipo: String? = null)

    val pendiente = MutableStateFlow<Destino?>(null)

    private val RESULTADOS = setOf("ok", "cancelado", "caducado", "error")

    // Solo un número de reserva y valores conocidos: el enlace o el aviso lo puede abrir cualquiera.
    private fun numero(texto: String?) = texto?.takeIf { it.length <= 10 && it.isNotEmpty() && it.all(Char::isDigit) }?.toIntOrNull()

    fun recibir(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "mesalista" && uri.host == "reserva") {
            val id = numero(uri.pathSegments.firstOrNull())
            val pago = uri.getQueryParameter("pago")?.takeIf { it in RESULTADOS }
            if (id != null || pago != null) pendiente.value = Destino(id, pago)
            return
        }
        val tipo = runCatching { intent?.getStringExtra("type") }.getOrNull()?.takeIf { it in Avisos.TIPOS }
        if (tipo != null) {
            val id = numero(runCatching { intent?.getStringExtra("reservation_id") }.getOrNull())
            pendiente.value = Destino(id, null, tipo)
        }
    }

    /** Texto del resultado de un pago para mostrarlo al volver de PayPal. */
    fun textoPago(resultado: String) = when (resultado) {
        "ok" -> "Señal pagada: tu reserva de grupo está confirmada. Te hemos enviado un correo."
        "cancelado" -> "Has cancelado el pago. Aún puedes pagar antes de que termine el plazo."
        "caducado" -> "El plazo para pagar había terminado: no se te ha cobrado nada."
        else -> "No se ha podido completar el pago. No se te ha cobrado nada; inténtalo de nuevo."
    }
}
