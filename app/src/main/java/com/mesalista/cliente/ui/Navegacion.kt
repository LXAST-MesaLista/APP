package com.mesalista.cliente.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mesalista.cliente.App
import com.mesalista.cliente.ui.acceso.BienvenidaPantalla
import com.mesalista.cliente.ui.acceso.EntrarPantalla
import com.mesalista.cliente.ui.acceso.RecuperarPantalla
import com.mesalista.cliente.ui.acceso.RegistroPantalla
import com.mesalista.cliente.ui.inicio.InicioPantalla

/**
 * Raíz de la app: sin sesión, las pantallas de acceso; con sesión, las del
 * cliente. Si la sesión caduca (401), la app vuelve sola a «Entrar».
 */
@Composable
fun RaizApp() {
    val usuario by App.sesion.usuario.collectAsState()
    if (usuario == null) AccesoNav() else PrincipalNav()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccesoNav() {
    val nav = rememberNavController()
    val actual by nav.currentBackStackEntryAsState()
    val ruta = actual?.destination?.route
    // Si había un aviso (sesión caducada), se empieza directamente en «Entrar».
    val inicio = if (App.sesion.aviso.value != null) "entrar" else "bienvenida"
    Scaffold(
        topBar = {
            if (ruta != null && ruta != inicio) {
                TopAppBar(
                    title = { Text("MesaLista") },
                    navigationIcon = {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                )
            }
        },
    ) { margen ->
        NavHost(nav, startDestination = inicio, modifier = Modifier.padding(margen)) {
            composable("bienvenida") {
                BienvenidaPantalla(irAEntrar = { nav.navigate("entrar") }, irARegistro = { nav.navigate("registro") })
            }
            composable("entrar") {
                EntrarPantalla(irARegistro = { nav.navigate("registro") }, irARecuperar = { nav.navigate("recuperar") })
            }
            composable("registro") {
                RegistroPantalla(irAEntrar = { nav.navigate("entrar") { popUpTo("bienvenida") } })
            }
            composable("recuperar") {
                RecuperarPantalla(irAEntrar = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun PrincipalNav() {
    val nav = rememberNavController()
    Scaffold { margen ->
        NavHost(nav, startDestination = "inicio", modifier = Modifier.padding(margen)) {
            composable("inicio") { InicioPantalla() }
        }
    }
}
