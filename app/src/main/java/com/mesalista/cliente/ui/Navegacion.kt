package com.mesalista.cliente.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mesalista.cliente.App
import com.mesalista.cliente.datos.Avisos
import com.mesalista.cliente.datos.Enlaces
import com.mesalista.cliente.ui.acceso.BienvenidaPantalla
import com.mesalista.cliente.ui.acceso.EntrarPantalla
import com.mesalista.cliente.ui.acceso.RecuperarPantalla
import com.mesalista.cliente.ui.acceso.RegistroPantalla
import com.mesalista.cliente.ui.comun.Mensajes
import com.mesalista.cliente.ui.cuenta.CuentaPantalla
import com.mesalista.cliente.ui.inicio.InicioPantalla
import com.mesalista.cliente.ui.reservas.ConfirmarPantalla
import com.mesalista.cliente.ui.reservas.DetallePantalla
import com.mesalista.cliente.ui.reservas.GrupoPantalla
import com.mesalista.cliente.ui.reservas.VincularPantalla
import com.mesalista.cliente.ui.reservas.ModificarPantalla
import com.mesalista.cliente.ui.reservas.MisReservasPantalla
import com.mesalista.cliente.ui.reservas.ReservarPantalla

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
    val inicio = remember { if (App.sesion.aviso.value != null) "entrar" else "bienvenida" }
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
                RegistroPantalla(irAEntrar = { nav.navigate("entrar") { popUpTo(inicio) } })
            }
            composable("recuperar") {
                RecuperarPantalla(irAEntrar = { nav.popBackStack() })
            }
        }
    }
}

/** Secciones de la barra inferior. */
private enum class Seccion(val ruta: String, val texto: String, val icono: ImageVector) {
    Inicio("inicio", "Inicio", Icons.Filled.Home),
    Reservar("reservar", "Reservar", Icons.Filled.DateRange),
    Reservas("reservas", "Mis reservas", Icons.AutoMirrored.Filled.List),
    Cuenta("cuenta", "Cuenta", Icons.Filled.Person),
}

/** Título de la barra superior de cada pantalla. */
private fun titulo(ruta: String?) = when {
    ruta == null -> "MesaLista"
    ruta.startsWith("confirmar") -> "Confirmar reserva"
    ruta.startsWith("reserva/") -> "Reserva"
    ruta.startsWith("modificar") -> "Modificar reserva"
    ruta == "grupos" -> "Grupo grande"
    ruta == "vincular" -> "Reserva del local"
    else -> Seccion.entries.firstOrNull { it.ruta == ruta }?.texto ?: "MesaLista"
}

/** Ir a una sección de la barra inferior (sin apilar pantallas repetidas). */
fun NavHostController.irASeccion(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrincipalNav() {
    val nav = rememberNavController()
    val actual by nav.currentBackStackEntryAsState()
    val ruta = actual?.destination?.route
    val avisos = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        Mensajes.flujo.collect { avisos.showSnackbar(it) }
    }
    // Avisos push: se pide el permiso (Android 13+) una sola vez y se registra el móvil en el servidor.
    val contexto = LocalContext.current
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Avisos.activo && Build.VERSION.SDK_INT >= 33 && !Avisos.permisoPedido(contexto) &&
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Avisos.marcarPermisoPedido(contexto)
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        Avisos.registrar()
    }
    // Enlaces que abren una reserva: vuelta de PayPal y avisos push tocados.
    val destino by Enlaces.pendiente.collectAsState()
    LaunchedEffect(destino) {
        val d = destino ?: return@LaunchedEffect
        Enlaces.pendiente.value = null
        d.pago?.let { Mensajes.mostrar(Enlaces.textoPago(it)) }
        if (d.tipo != null) Avisos.cambios.tryEmit(Unit)   // los datos han cambiado: recargar
        val abierta = nav.currentBackStackEntry?.takeIf { it.destination.route == "reserva/{id}" }?.arguments?.getInt("id")
        when {
            d.reservaId != null && d.reservaId == abierta -> Unit   // ya está en pantalla y se recarga
            d.reservaId != null -> nav.navigate("reserva/${d.reservaId}")
            d.pago != null || d.tipo != null -> nav.irASeccion("reservas")
        }
    }
    val principal = Seccion.entries.any { it.ruta == ruta }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo(ruta)) },
                navigationIcon = {
                    if (!principal && nav.previousBackStackEntry != null) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                val seccionActual = when {
                    ruta == null -> null
                    ruta.startsWith("confirmar") -> Seccion.Reservar
                    ruta.startsWith("reserva/") || ruta.startsWith("modificar") || ruta == "grupos" || ruta == "vincular" -> Seccion.Reservas
                    else -> Seccion.entries.firstOrNull { it.ruta == ruta }
                }
                Seccion.entries.forEach { s ->
                    NavigationBarItem(
                        selected = s == seccionActual,
                        // En la sección actual, vuelve a su primera pantalla (p. ej. de una reserva a la lista).
                        onClick = { if (s != seccionActual || !nav.popBackStack(s.ruta, inclusive = false)) nav.irASeccion(s.ruta) },
                        icon = { Icon(s.icono, contentDescription = null) },
                        label = { Text(s.texto) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(avisos) },
    ) { margen ->
        NavHost(nav, startDestination = "inicio", modifier = Modifier.padding(margen)) {
            composable("inicio") {
                InicioPantalla(
                    irAReserva = { nav.navigate("reserva/$it") },
                    irAReservar = { nav.irASeccion("reservar") },
                    irAVincular = { nav.navigate("vincular") },
                )
            }
            composable("reservar") {
                ReservarPantalla(
                    irAConfirmar = { fecha, personas, hora -> nav.navigate("confirmar/$fecha/$personas/${android.net.Uri.encode(hora)}") },
                    irAGrupos = { nav.navigate("grupos") },
                )
            }
            composable("grupos") {
                GrupoPantalla(alEnviar = { nav.irASeccion("reservas") })
            }
            composable("vincular") {
                VincularPantalla(alVincular = { id -> nav.navigate("reserva/$id") { popUpTo("vincular") { inclusive = true } } })
            }
            composable(
                "confirmar/{fecha}/{personas}/{hora}",
                arguments = listOf(
                    navArgument("fecha") { type = NavType.StringType },
                    navArgument("personas") { type = NavType.IntType },
                    navArgument("hora") { type = NavType.StringType },
                ),
            ) {
                ConfirmarPantalla(
                    alConfirmar = { id -> nav.navigate("reserva/$id") { popUpTo("reservar") { inclusive = true } } },
                    alVolverAHoras = { nav.popBackStack() },
                )
            }
            composable("reservas") {
                MisReservasPantalla(
                    irAReserva = { nav.navigate("reserva/$it") },
                    irAGrupos = { nav.navigate("grupos") },
                    irAVincular = { nav.navigate("vincular") },
                )
            }
            composable("reserva/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                DetallePantalla(irAModificar = { nav.navigate("modificar/$it") })
            }
            composable("modificar/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
                ModificarPantalla(alGuardar = { nav.popBackStack() })
            }
            composable("cuenta") { CuentaPantalla() }
        }
    }
}
