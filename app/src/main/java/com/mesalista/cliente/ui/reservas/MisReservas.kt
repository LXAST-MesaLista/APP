package com.mesalista.cliente.ui.reservas

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mesalista.cliente.App
import com.mesalista.cliente.datos.Avisos
import com.mesalista.cliente.datos.ApiError
import com.mesalista.cliente.datos.Reserva
import com.mesalista.cliente.datos.SolicitudGrupo
import com.mesalista.cliente.ui.comun.Aviso
import com.mesalista.cliente.ui.comun.BotonSecundario
import com.mesalista.cliente.ui.comun.FilaBotones
import com.mesalista.cliente.ui.comun.Mensajes
import com.mesalista.cliente.ui.comun.Pagina
import com.mesalista.cliente.ui.comun.Seccion
import com.mesalista.cliente.ui.comun.TarjetaReserva
import com.mesalista.cliente.ui.comun.Titulo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Mis reservas (RF-14) y solicitudes de grupo (RF-43). */
class MisReservasVM : ViewModel() {
    var reservas by mutableStateOf<List<Reserva>?>(null)
        private set
    var solicitudes by mutableStateOf<List<SolicitudGrupo>>(emptyList())
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun cargar() {
        viewModelScope.launch {
            error = null
            try {
                coroutineScope {
                    val r = async { App.api.get("/reservations") }
                    val g = async { App.api.get("/group-requests") }
                    reservas = Reserva.lista(r.await())
                    solicitudes = SolicitudGrupo.lista(g.await())
                }
            } catch (e: ApiError) {
                error = e.texto
            }
        }
    }

    fun cancelarSolicitud(id: Int) {
        viewModelScope.launch {
            try {
                solicitudes = SolicitudGrupo.lista(App.api.post("/group-requests/$id/cancel"))
                Mensajes.mostrar("Solicitud cancelada.")
            } catch (e: ApiError) {
                error = e.texto
            }
        }
    }
}

@Composable
fun MisReservasPantalla(irAReserva: (Int) -> Unit, irAGrupos: () -> Unit, irAVincular: () -> Unit, vm: MisReservasVM = viewModel()) {
    LaunchedEffect(Unit) { vm.cargar() }
    // Ha llegado un aviso push con la pantalla abierta: se recargan los datos.
    LaunchedEffect(Unit) { Avisos.cambios.collect { vm.cargar() } }
    val lista = vm.reservas
    Pagina {
        Titulo("Mis reservas")
        vm.error?.let {
            Aviso(it)
            BotonSecundario("Reintentar") { vm.cargar() }
        }
        if (lista == null) {
            if (vm.error == null) CircularProgressIndicator()
            return@Pagina
        }
        val reparto = Reparto(lista, vm.solicitudes)
        Seccion("Próximas")
        if (reparto.proximas.isEmpty()) {
            Text("No tienes reservas pendientes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        reparto.proximas.forEach { r -> TarjetaReserva(r) { irAReserva(r.id) } }

        Seccion("Solicitudes de grupo")
        if (reparto.esperando.isEmpty()) {
            Text("No tienes solicitudes de grupo pendientes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        reparto.esperando.forEach { g ->
            TarjetaSolicitud(g, irAReserva, vm::cancelarSolicitud, pendientePago = g.estado == "accepted")
        }

        if (reparto.noAceptadas.isNotEmpty()) {
            Seccion("No aceptadas")
            reparto.noAceptadas.forEach { g -> TarjetaSolicitud(g, irAReserva, vm::cancelarSolicitud) }
        }

        FilaBotones {
            BotonSecundario("Pedir para un grupo", Modifier.weight(1f), alPulsar = irAGrupos)
            BotonSecundario("Reserva del local", Modifier.weight(1f), alPulsar = irAVincular)
        }

        if (reparto.anteriores.isNotEmpty()) {
            Seccion("Anteriores")
            reparto.anteriores.forEach { x ->
                when (x) {
                    is Reserva -> TarjetaReserva(x) { irAReserva(x.id) }
                    is SolicitudGrupo -> TarjetaSolicitud(x, irAReserva, vm::cancelarSolicitud)
                }
            }
        }
    }
}

/**
 * Reparte las reservas y las solicitudes de grupo entre las secciones, sin
 * que nada salga dos veces (igual que la web, src/paginas/cliente/reservas.js):
 *  - Solicitudes de grupo: las que esperan respuesta del local y las aceptadas
 *    que falta pagar (su reserva no sale también en «Próximas»).
 *  - No aceptadas: las que el local ha rechazado, con su motivo.
 *  - Próximas: las demás reservas pendientes.
 *  - Anteriores: las reservas terminadas y las solicitudes caducadas o
 *    canceladas antes de aceptarse. De una caducada solo sale la solicitud, no
 *    la reserva que se liberó. Las aceptadas ya confirmadas, o canceladas
 *    después, se ven por su reserva.
 */
private class Reparto(reservas: List<Reserva>, solicitudes: List<SolicitudGrupo>) {
    private val porId = reservas.associateBy { it.id }
    private fun reservaDe(g: SolicitudGrupo) = g.reservaId?.let { porId[it] }

    val esperando = solicitudes.filter { g ->
        g.estado == "pending" || (g.estado == "accepted" && reservaDe(g)?.estado == "pending_payment")
    }
    private val enSolicitudes = esperando.mapNotNull { it.reservaId }.toSet()

    val noAceptadas = solicitudes.filter { it.estado == "rejected" }.sortedByDescending { "${it.fecha} ${it.hora}" }

    private val terminadas = solicitudes.filter { g -> g.estado == "expired" || (g.estado == "cancelled" && g.reservaId == null) }
    private val tapadas = terminadas.mapNotNull { it.reservaId }.toSet()

    val proximas = reservas.filter { it.pendiente && it.id !in enSolicitudes }

    /** Reservas (Reserva) y solicitudes (SolicitudGrupo) terminadas, de la más reciente a la más antigua. */
    val anteriores: List<Any> = (reservas.filter { !it.pendiente && it.id !in tapadas } + terminadas)
        .sortedByDescending { x ->
            when (x) {
                is Reserva -> "${x.fecha} ${x.hora}"
                is SolicitudGrupo -> "${x.fecha} ${x.hora}"
                else -> ""
            }
        }
}

