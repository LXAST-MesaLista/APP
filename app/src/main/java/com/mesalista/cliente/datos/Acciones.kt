package com.mesalista.cliente.datos

import com.mesalista.cliente.App

/** Acciones que se usan desde varias pantallas. */
object Acciones {
    /** Antes de cerrar la sesión: dar de baja el móvil para los avisos (DELETE /me/devices). */
    suspend fun antesDeSalir() {
        Avisos.darDeBaja()
    }

    /** Cerrar la sesión (RF-03): en el servidor y en el móvil. Si ya había caducado, da igual. */
    suspend fun salir() {
        runCatching { antesDeSalir() }
        runCatching { App.api.post("/auth/logout") }
        App.sesion.salir()
    }
}
