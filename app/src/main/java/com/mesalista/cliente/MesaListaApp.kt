package com.mesalista.cliente

import android.app.Application
import com.mesalista.cliente.datos.Api
import com.mesalista.cliente.datos.Avisos
import com.mesalista.cliente.datos.Sesion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Arranque de la app: crea una sola vez la sesión y el cliente de la API.
 * Las pantallas los usan a través de [App].
 */
class MesaListaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        App.sesion = Sesion(this)
        App.api = Api(BuildConfig.API, App.sesion)
        if (Avisos.activo) {
            Avisos.crearCanal(this)
            // Al quedarse sin sesión (cerrar sesión, sesión caducada o cuenta
            // eliminada), el móvil deja de recibir avisos de esa cuenta.
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                var conSesion = App.sesion.usuario.value != null
                App.sesion.usuario.collect { u ->
                    if (conSesion && u == null) Avisos.olvidarToken()
                    conSesion = u != null
                }
            }
        }
    }
}

/** Lo que comparten todas las pantallas. */
object App {
    lateinit var sesion: Sesion
    lateinit var api: Api
}
