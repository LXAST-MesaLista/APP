package com.mesalista.cliente

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mesalista.cliente.datos.Avisos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servicio de Firebase Cloud Messaging. Android lo arranca cuando FCM da un
 * token nuevo o cuando llega un aviso con la app abierta (con la app cerrada,
 * el aviso lo muestra el sistema en el canal «Reservas»).
 */
class AvisosServicio : FirebaseMessagingService() {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        alcance.launch { Avisos.registrar(token) }
    }

    override fun onMessageReceived(mensaje: RemoteMessage) {
        val titulo = mensaje.notification?.title ?: mensaje.data["title"] ?: getString(R.string.app_name)
        val texto = mensaje.notification?.body ?: mensaje.data["body"].orEmpty()
        Avisos.mostrar(this, titulo, texto, mensaje.data)
        Avisos.cambios.tryEmit(Unit)
    }

    override fun onDestroy() {
        alcance.cancel()
        super.onDestroy()
    }
}
