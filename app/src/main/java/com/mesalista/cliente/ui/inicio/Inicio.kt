package com.mesalista.cliente.ui.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mesalista.cliente.App
import com.mesalista.cliente.datos.Avisos
import com.mesalista.cliente.datos.ApiError
import com.mesalista.cliente.datos.AvisoRestaurante
import com.mesalista.cliente.datos.Reserva
import com.mesalista.cliente.datos.Usuario
import com.mesalista.cliente.ui.comun.Aviso
import com.mesalista.cliente.ui.comun.BotonPrincipal
import com.mesalista.cliente.ui.comun.BotonSecundario
import com.mesalista.cliente.ui.comun.Pagina
import com.mesalista.cliente.ui.comun.Seccion
import com.mesalista.cliente.ui.comun.TarjetaReserva
import com.mesalista.cliente.ui.comun.Titulo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Inicio: avisos del restaurante (RF-32) y próximas reservas. */
class InicioVM : ViewModel() {
    var cargando by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var avisos by mutableStateOf<List<AvisoRestaurante>>(emptyList())
        private set
    var proximas by mutableStateOf<List<Reserva>>(emptyList())
        private set

    fun cargar() {
        viewModelScope.launch {
            error = null
            try {
                // Las tres a la vez. Si la sesión ya no vale (401), la app vuelve sola a «Entrar».
                // coroutineScope: si una falla, el error llega aquí sin cerrar la app.
                coroutineScope {
                    val yo = async { App.api.get("/me") }
                    val n = async { App.api.get("/me/notifications") }
                    val r = async { App.api.get("/reservations") }
                    App.sesion.actualizar(Usuario.de(yo.await().getJSONObject("user")))
                    avisos = AvisoRestaurante.lista(n.await())
                    proximas = Reserva.lista(r.await()).filter { it.pendiente }
                }
            } catch (e: ApiError) {
                error = e.texto
            } finally {
                cargando = false
            }
        }
    }

    fun avisosLeidos() {
        viewModelScope.launch {
            runCatching { App.api.post("/me/notifications/read") }
            avisos = emptyList()
        }
    }
}

@Composable
fun InicioPantalla(irAReserva: (Int) -> Unit, irAReservar: () -> Unit, irAVincular: () -> Unit, vm: InicioVM = viewModel()) {
    val usuario by App.sesion.usuario.collectAsState()
    LaunchedEffect(Unit) { vm.cargar() }
    // Ha llegado un aviso push con la pantalla abierta: se recargan los datos.
    LaunchedEffect(Unit) { Avisos.cambios.collect { vm.cargar() } }
    Pagina {
        Titulo("Hola, ${usuario?.nombre?.substringBefore(' ').orEmpty()}")
        if (vm.avisos.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Avisos del restaurante", style = MaterialTheme.typography.titleMedium)
                    vm.avisos.forEach { a ->
                        Text("• ${a.mensaje}", style = MaterialTheme.typography.bodyMedium)
                        a.reservaId?.let { id -> TextButton(onClick = { irAReserva(id) }) { Text("Ver la reserva") } }
                    }
                    FilledTonalButton(onClick = vm::avisosLeidos) { Text("Entendido") }
                }
            }
        }
        BotonPrincipal("Reservar mesa", cargando = false, alPulsar = irAReservar)
        Seccion("Próximas reservas")
        vm.error?.let { Aviso(it) }
        when {
            vm.cargando -> CircularProgressIndicator()
            vm.proximas.isEmpty() && vm.error == null -> Text(
                "No tienes ninguna reserva pendiente.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(),
            )
            else -> vm.proximas.forEach { r -> TarjetaReserva(r) { irAReserva(r.id) } }
        }
        BotonSecundario("Añadir una reserva hecha en el local", alPulsar = irAVincular)
    }
}
