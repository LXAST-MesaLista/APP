package com.mesalista.cliente.ui.reservas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mesalista.cliente.App
import com.mesalista.cliente.datos.ApiError
import com.mesalista.cliente.datos.Config
import com.mesalista.cliente.datos.Datos
import com.mesalista.cliente.datos.Formato
import com.mesalista.cliente.ui.comun.Aviso
import com.mesalista.cliente.ui.comun.BotonPrincipal
import com.mesalista.cliente.ui.comun.Mensajes
import com.mesalista.cliente.ui.comun.Pagina
import com.mesalista.cliente.ui.comun.Titulo
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/**
 * Confirmar la reserva (RF-07 a RF-13, RP-04). El request_id se crea al abrir
 * la pantalla y se reutiliza si hay que reenviar: así un reintento con mala
 * cobertura no crea dos reservas (RF-10; el servidor responde «replayed»).
 */
class ConfirmarVM(estado: SavedStateHandle) : ViewModel() {
    val fecha: LocalDate = LocalDate.parse(checkNotNull(estado.get<String>("fecha")))
    val personas: Int = checkNotNull(estado.get<Int>("personas"))
    val hora: String = checkNotNull(estado.get<String>("hora"))
    private val requestId: String = estado.get<String>("request_id") ?: UUID.randomUUID().toString().also { estado["request_id"] = it }

    val extras = Extras()
    var cfg by mutableStateOf<Config?>(null)
        private set
    var enviando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var errorNota by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch { runCatching { cfg = Datos.config() }.onFailure { error = (it as? ApiError)?.texto } }
    }

    fun confirmar(alConfirmar: (Int) -> Unit, alVolverAHoras: () -> Unit) {
        if (enviando) return
        viewModelScope.launch {
            enviando = true
            error = null
            errorNota = null
            try {
                val cuerpo = extras.json(
                    JSONObject().put("request_id", requestId).put("date", fecha.toString()).put("time", hora).put("party", personas)
                )
                val r = App.api.post("/reservations", cuerpo)
                Mensajes.mostrar(
                    if (r.optBoolean("replayed")) "Tu reserva ya estaba registrada."
                    else "¡Reserva confirmada! Tienes el QR en «Mis reservas»."
                )
                alConfirmar(r.getJSONObject("reservation").getInt("id"))
            } catch (e: ApiError) {
                when {
                    // Otra persona se ha adelantado: a elegir otra hora.
                    e.codigo == "conflict" -> {
                        Mensajes.mostrar(e.texto)
                        alVolverAHoras()
                    }
                    e.campo == "note" -> errorNota = e.texto
                    else -> error = e.texto
                }
            } finally {
                enviando = false
            }
        }
    }
}

@Composable
fun ConfirmarPantalla(alConfirmar: (Int) -> Unit, alVolverAHoras: () -> Unit, vm: ConfirmarVM = viewModel()) {
    Pagina {
        Titulo("Confirmar reserva")
        Resumen(vm.fecha, vm.hora, vm.personas)
        vm.error?.let { Aviso(it) }
        val cfg = vm.cfg
        if (cfg == null) {
            if (vm.error == null) CircularProgressIndicator()
            return@Pagina
        }
        FormularioExtras(cfg, vm.extras, vm.errorNota)
        BotonPrincipal("Confirmar reserva", vm.enviando) { vm.confirmar(alConfirmar, alVolverAHoras) }
        Text(
            "Te avisaremos en la app de cualquier cambio. Puedes cambiarla o cancelarla desde «Mis reservas» hasta unas horas antes. " +
                "Si no vienes y no cancelas, la reserva queda como «no presentada».",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Día, hora y comensales en una tarjeta. */
@Composable
fun Resumen(fecha: LocalDate, hora: String, personas: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Dato("Día", Formato.fechaCorta(fecha))
            Dato("Hora", hora)
            Dato("Comensales", personas.toString())
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.titleMedium)
    }
}
