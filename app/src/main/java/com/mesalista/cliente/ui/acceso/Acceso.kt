package com.mesalista.cliente.ui.acceso

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mesalista.cliente.App
import com.mesalista.cliente.R
import com.mesalista.cliente.datos.ApiError
import com.mesalista.cliente.ui.comun.Aviso
import com.mesalista.cliente.ui.comun.BotonPrincipal
import com.mesalista.cliente.ui.comun.BotonSecundario
import com.mesalista.cliente.ui.comun.Campo
import com.mesalista.cliente.ui.comun.Pagina
import com.mesalista.cliente.ui.comun.Titulo
import com.mesalista.cliente.ui.tema.Marca
import kotlinx.coroutines.launch
import org.json.JSONObject

// ---------------------------------------------------------------------------
// Bienvenida
// ---------------------------------------------------------------------------

@Composable
fun BienvenidaPantalla(irAEntrar: () -> Unit, irARegistro: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painterResource(R.drawable.ic_launcher_foreground), contentDescription = null,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)).background(Marca),
            )
            Text("MesaLista", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Reserva tu mesa sin llamar, cámbiala o cancélala cuando quieras y enseña tu QR al llegar.",
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            BotonPrincipal("Entrar", cargando = false, alPulsar = irAEntrar)
            BotonSecundario("Crear cuenta", alPulsar = irARegistro)
        }
    }
}

// ---------------------------------------------------------------------------
// Entrar (RF-02)
// ---------------------------------------------------------------------------

class EntrarVM : ViewModel() {
    var email by mutableStateOf("")
    var clave by mutableStateOf("")
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun entrar() {
        if (cargando) return
        viewModelScope.launch {
            cargando = true
            error = null
            try {
                val r = App.api.post("/auth/login", JSONObject().put("email", email.trim()).put("password", clave))
                App.sesion.entrar(r)   // la app cambia sola a las pantallas del cliente
            } catch (e: ApiError) {
                error = e.texto
                clave = ""
            } finally {
                cargando = false
            }
        }
    }
}

@Composable
fun EntrarPantalla(irARegistro: () -> Unit, irARecuperar: () -> Unit, vm: EntrarVM = viewModel()) {
    val aviso by App.sesion.aviso.collectAsState()
    Pagina {
        Titulo("Iniciar sesión")
        aviso?.let { if (vm.error == null) Aviso(it, "aviso") }
        vm.error?.let { Aviso(it) }
        Campo(vm.email, { vm.email = it }, "Correo electrónico", teclado = KeyboardType.Email)
        Campo(vm.clave, { vm.clave = it }, "Contraseña", clave = true, ultimo = true, alTerminar = vm::entrar, maximo = 128)
        BotonPrincipal("Entrar", vm.cargando, activo = vm.email.isNotBlank() && vm.clave.isNotEmpty(), alPulsar = vm::entrar)
        TextButton(onClick = irARecuperar, modifier = Modifier.fillMaxWidth()) { Text("¿Has olvidado tu contraseña?") }
        TextButton(onClick = irARegistro, modifier = Modifier.fillMaxWidth()) { Text("¿No tienes cuenta? Crea una") }
    }
}

// ---------------------------------------------------------------------------
// Crear cuenta (RF-01): el servidor abre la sesión al crearla.
// ---------------------------------------------------------------------------

class RegistroVM : ViewModel() {
    var nombre by mutableStateOf("")
    var email by mutableStateOf("")
    var telefono by mutableStateOf("")
    var clave by mutableStateOf("")
    var clave2 by mutableStateOf("")
    var cargando by mutableStateOf(false)
        private set
    var errores by mutableStateOf<Map<String, String>>(emptyMap())
        private set

    fun crear() {
        if (cargando) return
        if (clave != clave2) {
            errores = mapOf("password2" to "Las contraseñas no coinciden.")
            return
        }
        viewModelScope.launch {
            cargando = true
            errores = emptyMap()
            try {
                val r = App.api.post(
                    "/auth/register",
                    JSONObject().put("name", nombre.trim()).put("email", email.trim()).put("phone", telefono.trim()).put("password", clave),
                )
                App.sesion.entrar(r)
            } catch (e: ApiError) {
                errores = mapOf((e.campo ?: "_") to e.texto)
            } finally {
                cargando = false
            }
        }
    }
}

@Composable
fun RegistroPantalla(irAEntrar: () -> Unit, vm: RegistroVM = viewModel()) {
    val e = vm.errores
    Pagina {
        Titulo("Crear cuenta")
        e["_"]?.let { Aviso(it) }
        Campo(vm.nombre, { vm.nombre = it }, "Nombre", error = e["name"], maximo = 80)
        Campo(vm.email, { vm.email = it }, "Correo electrónico", error = e["email"], teclado = KeyboardType.Email)
        Campo(
            vm.telefono, { vm.telefono = it }, "Teléfono", error = e["phone"], teclado = KeyboardType.Phone, maximo = 25,
            ayuda = "Solo se usa para avisarte de una incidencia con tu reserva y para añadir reservas hechas en el local.",
        )
        Campo(vm.clave, { vm.clave = it }, "Contraseña", error = e["password"], clave = true, maximo = 128,
            ayuda = "Al menos 10 caracteres. Evita contraseñas comunes.")
        Campo(vm.clave2, { vm.clave2 = it }, "Repite la contraseña", error = e["password2"], clave = true, maximo = 128,
            ultimo = true, alTerminar = vm::crear)
        BotonPrincipal(
            "Crear cuenta", vm.cargando,
            activo = vm.nombre.isNotBlank() && vm.email.isNotBlank() && vm.telefono.isNotBlank() && vm.clave.isNotEmpty(),
            alPulsar = vm::crear,
        )
        TextButton(onClick = irAEntrar, modifier = Modifier.fillMaxWidth()) { Text("¿Ya tienes cuenta? Inicia sesión") }
    }
}

// ---------------------------------------------------------------------------
// ¿Has olvidado tu contraseña? (RF-40). El enlace del correo abre la web,
// donde se elige la contraseña nueva.
// ---------------------------------------------------------------------------

class RecuperarVM : ViewModel() {
    var email by mutableStateOf("")
    var cargando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var enviado by mutableStateOf(false)
        private set

    fun enviar() {
        if (cargando) return
        viewModelScope.launch {
            cargando = true
            error = null
            try {
                App.api.post("/auth/forgot", JSONObject().put("email", email.trim()))
                enviado = true
            } catch (e: ApiError) {
                error = e.texto
            } finally {
                cargando = false
            }
        }
    }
}

@Composable
fun RecuperarPantalla(irAEntrar: () -> Unit, vm: RecuperarVM = viewModel()) {
    Pagina {
        if (vm.enviado) {
            Titulo("Revisa tu correo")
            Aviso(
                "Si ${vm.email.trim()} tiene una cuenta en MesaLista, te hemos enviado un enlace para elegir una contraseña nueva. Caduca en 30 minutos.",
                "ok",
            )
            Text(
                "¿No te llega? Mira en la carpeta de correo no deseado o vuelve a pedirlo dentro de unos minutos.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BotonPrincipal("Volver a iniciar sesión", cargando = false, alPulsar = irAEntrar)
        } else {
            Titulo("¿Has olvidado tu contraseña?", "Te enviaremos un enlace para elegir otra.")
            vm.error?.let { Aviso(it) }
            Campo(vm.email, { vm.email = it }, "Correo electrónico de tu cuenta", teclado = KeyboardType.Email, ultimo = true, alTerminar = vm::enviar)
            BotonPrincipal("Enviar enlace", vm.cargando, activo = vm.email.isNotBlank(), alPulsar = vm::enviar)
        }
    }
}
