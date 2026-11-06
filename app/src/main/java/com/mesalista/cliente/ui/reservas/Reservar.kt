package com.mesalista.cliente.ui.reservas

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mesalista.cliente.App
import com.mesalista.cliente.datos.ApiError
import com.mesalista.cliente.datos.Config
import com.mesalista.cliente.datos.Datos
import com.mesalista.cliente.datos.Formato
import com.mesalista.cliente.ui.comun.Aviso
import com.mesalista.cliente.ui.comun.BotonSecundario
import com.mesalista.cliente.ui.comun.Pagina
import com.mesalista.cliente.ui.comun.Seccion
import com.mesalista.cliente.ui.comun.Titulo
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.time.LocalDate

/** Reservar (RF-06): día y comensales → horas libres. */
class ReservarVM : ViewModel() {
    var cfg by mutableStateOf<Config?>(null)
        private set
    var fecha by mutableStateOf(LocalDate.now())
        private set
    var personas by mutableStateOf(2)
        private set
    var horas by mutableStateOf<List<String>>(emptyList())
        private set
    var cargando by mutableStateOf(true)
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
                personas = personas.coerceIn(c.minPersonas, c.maxPersonas)
                buscar()
            } catch (e: ApiError) {
                error = e.texto
                cargando = false
            }
        }
    }

    fun elegirDia(d: LocalDate) {
        fecha = d
        buscar()
    }

    fun elegirPersonas(n: Int) {
        personas = n
        buscar()
    }

    /** Horas libres del día y comensales elegidos; si se cambia antes de llegar, se descarta la anterior. */
    fun buscar() {
        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            cargando = true
            error = null
            try {
                val r = App.api.get("/availability?date=$fecha&party=$personas")
                val a = r.optJSONArray("times") ?: JSONArray()
                horas = (0 until a.length()).map { a.getString(it) }
            } catch (e: ApiError) {
                horas = emptyList()
                error = e.texto
            } finally {
                cargando = false
            }
        }
    }
}

@Composable
fun ReservarPantalla(irAConfirmar: (String, Int, String) -> Unit, vm: ReservarVM = viewModel()) {
    val cfg = vm.cfg
    Pagina {
        Titulo("Reservar mesa", "Elige el día, cuántos sois y la hora.")
        if (cfg == null) {
            vm.error?.let { Aviso(it) } ?: CircularProgressIndicator()
            if (vm.error != null) BotonSecundario("Reintentar") { vm.buscar() }
            return@Pagina
        }
        Seccion("¿Qué día?")
        SelectorDia(cfg, vm.fecha, vm::elegirDia)
        Seccion("¿Cuántos sois?")
        SelectorPersonas(vm.personas, cfg.minPersonas, cfg.maxPersonas, vm::elegirPersonas)
        Text(
            "¿Sois más de ${cfg.maxPersonas}? Envía una solicitud de grupo (hasta ${cfg.maxGrupo} personas).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Seccion("Horas libres")
        when {
            vm.cargando -> CircularProgressIndicator()
            vm.error != null -> {
                Aviso(vm.error!!)
                BotonSecundario("Reintentar") { vm.buscar() }
            }
            vm.horas.isEmpty() -> Text(
                "No quedan horas libres para ${Formato.personas(vm.personas)} el ${Formato.fechaLarga(vm.fecha)}. " +
                    "Prueba otro día o con otro número de comensales.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {
                SelectorHora(vm.horas, null) { h -> irAConfirmar(vm.fecha.toString(), vm.personas, h) }
                Text("Elige una hora para confirmar la reserva. Cada mesa se reserva ${cfg.duracionMin} minutos.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
