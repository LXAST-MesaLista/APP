package com.mesalista.cliente.datos

import com.mesalista.cliente.App

/** Acciones que se usan desde varias pantallas. */
object Acciones {
    /** Cerrar la sesión (RF-03): en el servidor y en el móvil. Si ya había caducado, da igual. */
    suspend fun salir() {
        runCatching { App.api.post("/auth/logout") }
        App.sesion.salir()
    }
}
