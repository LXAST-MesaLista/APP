package com.mesalista.cliente.ui.reservas

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mesalista.cliente.App
import com.mesalista.cliente.datos.ApiError
import com.mesalista.cliente.ui.comun.Aviso
import com.mesalista.cliente.ui.comun.BotonPrincipal
import com.mesalista.cliente.ui.comun.Campo
import com.mesalista.cliente.ui.comun.Mensajes
import com.mesalista.cliente.ui.comun.Pagina
import com.mesalista.cliente.ui.comun.Titulo
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Añadir a la cuenta una reserva hecha en el local o por teléfono (RF-30),
 * con el código del ticket y el teléfono con el que se hizo.
 */
class VincularVM : ViewModel() {
    var codigo by mutableStateOf("")
    var telefono by mutableStateOf(App.sesion.usuario.value?.telefono.orEmpty())
    var enviando by mutableStateOf(false)
        private set
    var errores by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    fun vincular(alVincular: (Int) -> Unit) {
        viewModelScope.launch {
            enviando = true
            errores = emptyMap()
            try {
                val r = App.api.post("/reservations/link", JSONObject().put("code", codigo.trim()).put("phone", telefono.trim()))
                Mensajes.mostrar("Reserva añadida. A partir de ahora vale el QR de la app, no el del ticket.")
                alVincular(r.getJSONObject("reservation").getInt("id"))
            } catch (e: ApiError) {
                errores = mapOf((e.campo ?: "_") to e.texto)
            } finally {
                enviando = false
            }
        }
    }
}

@Composable
fun VincularPantalla(alVincular: (Int) -> Unit, vm: VincularVM = viewModel()) {
    Pagina {
        Titulo("Añadir una reserva del local")
        Text(
            "Si reservaste en el restaurante o por teléfono, añade la reserva a tu cuenta para verla, cambiarla o cancelarla desde aquí.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        vm.errores["_"]?.let { Aviso(it) }
        Campo(vm.codigo, { vm.codigo = it }, "Código de reserva", error = vm.errores["code"], mayusculas = true,
            ayuda = "Está impreso en el ticket, debajo del QR.", maximo = 14)
        Campo(vm.telefono, { vm.telefono = it }, "Teléfono con el que se hizo la reserva", error = vm.errores["phone"],
            teclado = KeyboardType.Phone, maximo = 25, ultimo = true, alTerminar = { vm.vincular(alVincular) })
        BotonPrincipal("Añadir a mis reservas", vm.enviando, activo = vm.codigo.isNotBlank() && vm.telefono.isNotBlank()) {
            vm.vincular(alVincular)
        }
    }
}
