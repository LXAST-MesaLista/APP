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
        val proximas = lista.filter { it.pendiente }
        val anteriores = lista.filterNot { it.pendiente }
        Seccion("Próximas")
        if (proximas.isEmpty()) {
            Text("No tienes reservas pendientes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        proximas.forEach { r -> TarjetaReserva(r) { irAReserva(r.id) } }

        Seccion("Solicitudes de grupo")
        if (vm.solicitudes.isEmpty()) {
            Text("No tienes solicitudes de grupo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        vm.solicitudes.forEach { g -> TarjetaSolicitud(g, irAReserva, vm::cancelarSolicitud) }

        FilaBotones {
            BotonSecundario("Pedir para un grupo", Modifier.weight(1f), alPulsar = irAGrupos)
            BotonSecundario("Reserva del local", Modifier.weight(1f), alPulsar = irAVincular)
        }

        if (anteriores.isNotEmpty()) {
            Seccion("Anteriores")
            anteriores.forEach { r -> TarjetaReserva(r) { irAReserva(r.id) } }
        }
    }
}

