package com.mesalista.cliente.ui.cuenta

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.mesalista.cliente.App
import com.mesalista.cliente.datos.Acciones
import com.mesalista.cliente.ui.comun.BotonSecundario
import com.mesalista.cliente.ui.comun.Pagina
import com.mesalista.cliente.ui.comun.Titulo
import kotlinx.coroutines.launch

/** Mi cuenta (en la fase 3: cambiar nombre y teléfono y eliminar la cuenta). */
@Composable
fun CuentaPantalla() {
    val usuario by App.sesion.usuario.collectAsState()
    val alcance = rememberCoroutineScope()
    Pagina {
        Titulo(usuario?.nombre.orEmpty(), usuario?.email)
        Text("Teléfono: ${usuario?.telefono.orEmpty()}", style = MaterialTheme.typography.bodyLarge)
        BotonSecundario("Cerrar sesión") { alcance.launch { Acciones.salir() } }
    }
}
