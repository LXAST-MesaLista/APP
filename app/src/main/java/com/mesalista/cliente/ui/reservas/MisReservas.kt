package com.mesalista.cliente.ui.reservas

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mesalista.cliente.App
import com.mesalista.cliente.datos.ApiError
import com.mesalista.cliente.datos.Reserva
import com.mesalista.cliente.ui.comun.Aviso
import com.mesalista.cliente.ui.comun.BotonSecundario
import com.mesalista.cliente.ui.comun.Pagina
import com.mesalista.cliente.ui.comun.Seccion
import com.mesalista.cliente.ui.comun.TarjetaReserva
import com.mesalista.cliente.ui.comun.Titulo
import kotlinx.coroutines.launch

/** Mis reservas (RF-14): próximas y anteriores. */
class MisReservasVM : ViewModel() {
    var reservas by mutableStateOf<List<Reserva>?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun cargar() {
        viewModelScope.launch {
            error = null
            try {
                reservas = Reserva.lista(App.api.get("/reservations"))
            } catch (e: ApiError) {
                error = e.texto
            }
        }
    }
}

@Composable
fun MisReservasPantalla(irAReserva: (Int) -> Unit, vm: MisReservasVM = viewModel()) {
    LaunchedEffect(Unit) { vm.cargar() }
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
        if (anteriores.isNotEmpty()) {
            Seccion("Anteriores")
            anteriores.forEach { r -> TarjetaReserva(r) { irAReserva(r.id) } }
        }
    }
}
