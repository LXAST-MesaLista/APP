package com.mesalista.cliente.ui.reservas

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mesalista.cliente.App
import com.mesalista.cliente.datos.ApiError
import com.mesalista.cliente.datos.Config
import com.mesalista.cliente.datos.Datos
import com.mesalista.cliente.datos.Formato
import com.mesalista.cliente.datos.Reserva
import com.mesalista.cliente.ui.comun.Aviso
import com.mesalista.cliente.ui.comun.BotonPrincipal
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
 * Modificar una reserva (RF-13, RF-15): día, comensales, hora y extras. En
 * las de grupo solo se cambian el menú y la nota (lo demás, por teléfono).
 */
class ModificarVM(estado: SavedStateHandle) : ViewModel() {
    private val id: Int = checkNotNull(estado.get<Int>("id"))
    var reserva by mutableStateOf<Reserva?>(null)
        private set
    var cfg by mutableStateOf<Config?>(null)
        private set
    var fecha by mutableStateOf(LocalDate.now())
        private set
    var personas by mutableStateOf(2)
        private set
    var hora by mutableStateOf<String?>(null)
    var horas by mutableStateOf<List<String>>(emptyList())
        private set
    var buscando by mutableStateOf(false)
        private set
    var extras by mutableStateOf(Extras())
        private set
    var guardando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var trabajo: Job? = null

    init {
        viewModelScope.launch {
            try {
                cfg = Datos.config()
                val r = Reserva.de(App.api.get("/reservations/$id").getJSONObject("reservation"))
                reserva = r
                fecha = r.fecha
                personas = r.personas
                hora = r.hora
                extras = Extras(r.opcionesMenu.toSet(), r.nota.orEmpty(), r.opcionesMenu.isNotEmpty() || r.nota != null)
                if (!r.grupo) buscar()
            } catch (e: ApiError) {
                error = e.texto
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

    private fun buscar() {
        val r = reserva ?: return
        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            buscando = true
            try {
                val a = App.api.get("/availability?date=$fecha&party=$personas").optJSONArray("times") ?: JSONArray()
                val lista = (0 until a.length()).map { a.getString(it) }.toMutableSet()
                if (fecha == r.fecha) lista += r.hora   // su propia hora sigue siendo una opción
                horas = lista.sorted()
                if (hora !in horas) hora = null
            } catch (e: ApiError) {
                horas = emptyList()
                error = e.texto
            } finally {
                buscando = false
            }
        }
    }

    fun guardar(alGuardar: () -> Unit) {
        val r = reserva ?: return
        if (!r.grupo && hora == null) {
            error = "Elige una hora."
            return
        }
        viewModelScope.launch {
            guardando = true
            error = null
            try {
                val cuerpo = extras.json(JSONObject())
                if (!r.grupo) cuerpo.put("date", fecha.toString()).put("time", hora).put("party", personas)
                val nueva = Reserva.de(App.api.patch("/reservations/$id", cuerpo).getJSONObject("reservation"))
                Mensajes.mostrar("Reserva modificada." + if (nueva.qr != r.qr) " Usa el QR nuevo: el anterior ya no vale." else "")
                alGuardar()
            } catch (e: ApiError) {
                error = e.texto
                if (e.codigo == "conflict") buscar()
            } finally {
                guardando = false
            }
        }
    }
}

@Composable
fun ModificarPantalla(alGuardar: () -> Unit, vm: ModificarVM = viewModel()) {
    val r = vm.reserva
    val cfg = vm.cfg
    Pagina {
        if (r == null || cfg == null) {
            vm.error?.let { Aviso(it) } ?: CircularProgressIndicator()
            return@Pagina
        }
        Titulo(if (r.grupo) "Cambiar menú o nota" else "Modificar reserva",
            "Ahora: ${Formato.fechaCorta(r.fecha)} a las ${r.hora}, ${Formato.personas(r.personas)}.")
        if (!r.grupo) {
            Seccion("Día")
            SelectorDia(cfg, vm.fecha, vm::elegirDia)
            Seccion("Comensales")
            SelectorPersonas(vm.personas, cfg.minPersonas, cfg.maxPersonas, vm::elegirPersonas)
            Seccion("Hora")
            when {
                vm.buscando -> CircularProgressIndicator()
                vm.horas.isEmpty() -> Text("No quedan horas libres ese día para ${Formato.personas(vm.personas)}.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> SelectorHora(vm.horas, vm.hora) { vm.hora = it }
            }
        }
        FormularioExtras(cfg, vm.extras)
        vm.error?.let { Aviso(it) }
        BotonPrincipal("Guardar cambios", vm.guardando) { vm.guardar(alGuardar) }
    }
}
