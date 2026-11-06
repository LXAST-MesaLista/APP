package com.mesalista.cliente.datos

import com.mesalista.cliente.App

/** Acciones que se usan desde varias pantallas. */
object Acciones {
    /** Lo que hay que hacer antes de cerrar la sesión (en la fase 4: dar de baja el móvil para los avisos). */
    suspend fun antesDeSalir() {
    }

    /** Cerrar la sesión (RF-03): en el servidor y en el móvil. Si ya había caducado, da igual. */
    suspend fun salir() {
        runCatching { antesDeSalir() }
        runCatching { App.api.post("/auth/logout") }
        App.sesion.salir()
    }
}
