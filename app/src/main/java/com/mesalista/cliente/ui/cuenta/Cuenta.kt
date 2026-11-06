package com.mesalista.cliente.ui.cuenta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mesalista.cliente.App
import com.mesalista.cliente.datos.Acciones
import com.mesalista.cliente.datos.ApiError
import com.mesalista.cliente.datos.Avisos
import com.mesalista.cliente.datos.Usuario
import com.mesalista.cliente.ui.comun.Aviso
import com.mesalista.cliente.ui.comun.BotonPrincipal
import com.mesalista.cliente.ui.comun.BotonSecundario
import com.mesalista.cliente.ui.comun.Campo
import com.mesalista.cliente.ui.comun.Mensajes
import com.mesalista.cliente.ui.comun.Pagina
import com.mesalista.cliente.ui.comun.Seccion
import com.mesalista.cliente.ui.comun.Titulo
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Mi cuenta (RF-04 y RF-05): nombre y teléfono, cerrar sesión y eliminar la cuenta. */
class CuentaVM : ViewModel() {
    private val u = App.sesion.usuario.value
    var nombre by mutableStateOf(u?.nombre.orEmpty())
    var telefono by mutableStateOf(u?.telefono.orEmpty())
    var clave by mutableStateOf("")
    var guardando by mutableStateOf(false)
        private set
    var eliminando by mutableStateOf(false)
        private set
    var errores by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var errorEliminar by mutableStateOf<String?>(null)
        private set

    fun guardar() {
        viewModelScope.launch {
            guardando = true
            errores = emptyMap()
            try {
                val r = App.api.patch("/me", JSONObject().put("name", nombre.trim()).put("phone", telefono.trim()))
                val nuevo = Usuario.de(r.getJSONObject("user"))
                App.sesion.actualizar(nuevo)
                nombre = nuevo.nombre
                telefono = nuevo.telefono
                Mensajes.mostrar("Datos guardados.")
            } catch (e: ApiError) {
                errores = mapOf((e.campo ?: "_") to e.texto)
            } finally {
                guardando = false
            }
        }
    }

    /** RF-05: el servidor cancela las reservas pendientes y borra los datos personales. */
    fun eliminar() {
        viewModelScope.launch {
            eliminando = true
            errorEliminar = null
            try {
                // El servidor borra también los dispositivos de la cuenta (avisos push).
                App.api.delete("/me", JSONObject().put("password", clave))
                App.sesion.salir("Tu cuenta se ha eliminado.")
            } catch (e: ApiError) {
                errorEliminar = e.texto
                clave = ""
            } finally {
                eliminando = false
            }
        }
    }
}

/** Estado de los avisos push en este móvil; se vuelve a mirar al volver de los ajustes. */
@Composable
private fun AvisosDelMovil() {
    val contexto = LocalContext.current
    var permitidos by remember { mutableStateOf(Avisos.permitidos(contexto)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permitidos = Avisos.permitidos(contexto) }
    Seccion("Avisos en este móvil")
    if (permitidos) {
        Aviso("Activados: te avisaremos cuando una reserva se confirme, cambie o se cancele.", tipo = "ok")
    } else {
        Text(
            "Los avisos están desactivados. Actívalos para saber al momento si el restaurante cancela o cambia una reserva.",
            style = MaterialTheme.typography.bodyMedium,
        )
        BotonSecundario("Activar avisos") { Avisos.abrirAjustes(contexto) }
    }
}

@Composable
fun CuentaPantalla(vm: CuentaVM = viewModel()) {
    val usuario by App.sesion.usuario.collectAsState()
    val alcance = rememberCoroutineScope()
    var confirmar by remember { mutableStateOf(false) }
    Pagina {
        Titulo("Mi cuenta", usuario?.email)
        vm.errores["_"]?.let { Aviso(it) }
        Campo(vm.nombre, { vm.nombre = it }, "Nombre", error = vm.errores["name"], maximo = 80)
        Campo(vm.telefono, { vm.telefono = it }, "Teléfono", error = vm.errores["phone"], teclado = KeyboardType.Phone, maximo = 25,
            ultimo = true, alTerminar = vm::guardar)
        BotonPrincipal("Guardar", vm.guardando, activo = vm.nombre.isNotBlank() && vm.telefono.isNotBlank(), alPulsar = vm::guardar)
        BotonSecundario("Cerrar sesión") { alcance.launch { Acciones.salir() } }

        if (Avisos.activo) AvisosDelMovil()

        Seccion("Eliminar cuenta")
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))) {
            Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Se cancelarán tus reservas pendientes y se borrarán tu nombre, tu correo y tu teléfono. No se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                vm.errorEliminar?.let { Aviso(it) }
                Campo(vm.clave, { vm.clave = it }, "Contraseña", clave = true, maximo = 128)
                OutlinedButton(onClick = { confirmar = true }, enabled = vm.clave.isNotEmpty() && !vm.eliminando, modifier = Modifier.fillMaxWidth()) {
                    Text("Eliminar mi cuenta", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (confirmar) {
        AlertDialog(
            onDismissRequest = { confirmar = false },
            title = { Text("¿Eliminar tu cuenta?") },
            text = { Text("Tus reservas pendientes se cancelarán. No se puede deshacer.") },
            confirmButton = { TextButton(onClick = { confirmar = false; vm.eliminar() }) { Text("Sí, eliminar") } },
            dismissButton = { TextButton(onClick = { confirmar = false }) { Text("No") } },
        )
    }
}
