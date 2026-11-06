package com.mesalista.cliente.ui.inicio

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.mesalista.cliente.App
import com.mesalista.cliente.datos.Acciones
import com.mesalista.cliente.datos.Usuario
import com.mesalista.cliente.ui.comun.BotonSecundario
import com.mesalista.cliente.ui.comun.Pagina
import com.mesalista.cliente.ui.comun.Titulo
import kotlinx.coroutines.launch

/** Inicio del cliente (en la fase 2: avisos del restaurante y próximas reservas). */
@Composable
fun InicioPantalla() {
    val usuario by App.sesion.usuario.collectAsState()
    val alcance = rememberCoroutineScope()
    // Al abrir: datos al día de la cuenta. Si la sesión ya no vale (401), la app vuelve a «Entrar».
    LaunchedEffect(Unit) {
        runCatching { App.sesion.actualizar(Usuario.de(App.api.get("/me").getJSONObject("user"))) }
    }
    Pagina {
        Titulo("Hola, ${usuario?.nombre?.substringBefore(' ').orEmpty()}")
        Text("Has entrado en MesaLista con ${usuario?.email.orEmpty()}.", style = MaterialTheme.typography.bodyLarge)
        BotonSecundario("Cerrar sesión") { alcance.launch { Acciones.salir() } }
    }
}
