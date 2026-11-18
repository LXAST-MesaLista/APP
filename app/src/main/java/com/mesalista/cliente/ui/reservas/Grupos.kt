package com.mesalista.cliente.ui.reservas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mesalista.cliente.App
import com.mesalista.cliente.datos.ApiError
import com.mesalista.cliente.datos.Config
import com.mesalista.cliente.datos.Datos
import com.mesalista.cliente.datos.Formato
import com.mesalista.cliente.datos.SolicitudGrupo
import com.mesalista.cliente.ui.comun.Aviso
import com.mesalista.cliente.ui.comun.BotonPrincipal
import com.mesalista.cliente.ui.comun.EstadoChip
import com.mesalista.cliente.ui.comun.Mensajes
import com.mesalista.cliente.ui.comun.Pagina
import com.mesalista.cliente.ui.comun.Seccion
import com.mesalista.cliente.ui.comun.Titulo
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Solicitud de mesa para un grupo (RF-43): más de max_party personas. El
 * restaurante la acepta o la rechaza; si la acepta y hay pasarela de pago,
 * el cliente paga una señal (RF-44) desde el detalle de la reserva.
 */
class GrupoVM : ViewModel() {
    var cfg by mutableStateOf<Config?>(null)
        private set
    var fecha by mutableStateOf(LocalDate.now())
        private set
    var personas by mutableStateOf(11)
    var hora by mutableStateOf<String?>(null)
    var horas by mutableStateOf<List<String>>(emptyList())
        private set
    var buscando by mutableStateOf(true)
        private set
    var nota by mutableStateOf("")
    var consentimiento by mutableStateOf(false)
    var enviando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var trabajo: Job? = null

    init {
        viewModelScope.launch {
            try {
                val c = Datos.config()
                cfg = c
                fecha = c.hoy
                personas = c.maxPersonas + 1
                buscar()
            } catch (e: ApiError) {
                error = e.texto
                buscando = false
            }
        }
    }

    fun elegirDia(d: LocalDate) {
        fecha = d
        buscar()
    }

    private fun buscar() {
        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            buscando = true
            hora = null
            try {
                val a = App.api.get("/group-requests/times?date=$fecha").optJSONArray("times") ?: JSONArray()
                horas = (0 until a.length()).map { a.getString(it) }
            } catch (e: ApiError) {
                horas = emptyList()
                error = e.texto
            } finally {
                buscando = false
            }
        }
    }

    fun enviar(alEnviar: () -> Unit) {
        val h = hora ?: run { error = "Elige una hora."; return }
        viewModelScope.launch {
            enviando = true
            error = null
            try {
                App.api.post(
                    "/group-requests",
                    JSONObject().put("date", fecha.toString()).put("time", h).put("party", personas)
                        .put("note", nota.trim().ifEmpty { JSONObject.NULL }).put("consent", consentimiento),
                )
                Mensajes.mostrar("Solicitud enviada. El restaurante te responderá en la app.")
                alEnviar()
            } catch (e: ApiError) {
                error = e.texto
            } finally {
                enviando = false
            }
        }
    }
}

@Composable
fun GrupoPantalla(alEnviar: () -> Unit, vm: GrupoVM = viewModel()) {
    val cfg = vm.cfg
    Pagina {
        Titulo("Solicitud de grupo")
        if (cfg == null) {
            vm.error?.let { Aviso(it) } ?: CircularProgressIndicator()
            return@Pagina
        }
        Text(
            "Para más de ${cfg.maxPersonas} personas el restaurante prepara las mesas a mano. Envía la solicitud y te responderá.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Seccion("Día")
        SelectorDia(cfg, vm.fecha, vm::elegirDia)
        Seccion("Comensales")
        SelectorPersonas(vm.personas, cfg.maxPersonas + 1, maxOf(cfg.maxPersonas + 1, cfg.maxGrupo)) { vm.personas = it }
        Seccion("Hora")
        when {
            vm.buscando -> CircularProgressIndicator()
            vm.horas.isEmpty() -> Text("Ese día no se puede pedir ninguna hora.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> SelectorHora(vm.horas, vm.hora) { vm.hora = it }
        }
        OutlinedTextField(
            value = vm.nota,
            onValueChange = { if (it.length <= 200) vm.nota = it },
            label = { Text("Nota para el restaurante (opcional)") },
            placeholder = { Text("Por ejemplo: cena de empresa, 2 personas celíacas") },
            supportingText = { Text("${vm.nota.length}/200") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth().toggleable(vm.consentimiento, role = Role.Checkbox) { vm.consentimiento = it },
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(checked = vm.consentimiento, onCheckedChange = null, modifier = Modifier.size(40.dp))
            Text(
                "Acepto que el restaurante use esta nota, que puede revelar datos de salud, solo para preparar la visita. " +
                    "Sin esta casilla la nota no se guarda.",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
        if (cfg.senalPorPersona > 0) {
            Aviso(
                "Si el restaurante la acepta, tendrás ${cfg.minutosSenal} minutos para pagar una señal de ${cfg.senalPorPersona} € " +
                    "por persona con PayPal. Se devuelve si cancelas dentro del plazo o si cancela el restaurante.",
                "aviso",
            )
        }
        vm.error?.let { Aviso(it) }
        BotonPrincipal("Enviar solicitud", vm.enviando, activo = vm.hora != null) { vm.enviar(alEnviar) }
    }
}

/**
 * Una solicitud de grupo en «Mis reservas», con «Cancelar» si aún no tiene
 * respuesta. pendientePago: aceptada y con la señal por pagar (su reserva no
 * sale en «Próximas», así que aquí va el botón para pagarla).
 */
@Composable
fun TarjetaSolicitud(g: SolicitudGrupo, irAReserva: (Int) -> Unit, alCancelar: (Int) -> Unit, pendientePago: Boolean = false) {
    var preguntar by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${Formato.fechaCorta(g.fecha)} · ${g.hora} · ${Formato.personas(g.personas)}", style = MaterialTheme.typography.titleSmall)
            if (pendientePago) {
                EstadoChip("pending_payment")
                Text("El restaurante ha aceptado tu grupo: paga la señal para confirmar la reserva.", style = MaterialTheme.typography.bodyMedium)
            } else {
                EstadoChip(g.estado, Formato.estadoSolicitud(g.estado))
            }
            g.respuesta?.let { Text("Restaurante: «$it»", style = MaterialTheme.typography.bodyMedium) }
            if (pendientePago && g.reservaId != null) {
                BotonPrincipal("Pagar la señal", cargando = false) { irAReserva(g.reservaId) }
            } else if (g.estado == "accepted" && g.reservaId != null) {
                TextButton(onClick = { irAReserva(g.reservaId) }) { Text("Ver la reserva ›") }
            }
            if (g.estado == "pending") {
                OutlinedButton(onClick = { preguntar = true }) { Text("Cancelar solicitud") }
            }
        }
    }
    if (preguntar) {
        AlertDialog(
            onDismissRequest = { preguntar = false },
            title = { Text("¿Cancelar la solicitud de grupo?") },
            confirmButton = { TextButton(onClick = { preguntar = false; alCancelar(g.id) }) { Text("Sí, cancelar") } },
            dismissButton = { TextButton(onClick = { preguntar = false }) { Text("No") } },
        )
    }
}
