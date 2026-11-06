package com.mesalista.cliente

import android.app.Application
import com.mesalista.cliente.datos.Api
import com.mesalista.cliente.datos.Sesion

/**
 * Arranque de la app: crea una sola vez la sesión y el cliente de la API.
 * Las pantallas los usan a través de [App].
 */
class MesaListaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        App.sesion = Sesion(this)
        App.api = Api(BuildConfig.API, App.sesion)
    }
}

/** Lo que comparten todas las pantallas. */
object App {
    lateinit var sesion: Sesion
    lateinit var api: Api
}
