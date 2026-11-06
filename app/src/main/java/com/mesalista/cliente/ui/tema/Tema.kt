package com.mesalista.cliente.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Colores de MesaLista, los mismos que la web: el color de la marca es
 * #A8432A. Para cambiar el diseño de la app, se cambian aquí.
 */
val Marca = Color(0xFFA8432A)

private val Claro = lightColorScheme(
    primary = Marca,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6E7E2),
    onPrimaryContainer = Color(0xFF5C2416),
    secondary = Color(0xFF6E645B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFE6DE),
    onSecondaryContainer = Color(0xFF2B231D),
    background = Color(0xFFFBF9F7),
    onBackground = Color(0xFF221C17),
    surface = Color(0xFFFBF9F7),
    onSurface = Color(0xFF221C17),
    surfaceVariant = Color(0xFFF1ECE7),
    onSurfaceVariant = Color(0xFF5E554D),
    surfaceContainer = Color(0xFFF4EFEA),
    surfaceContainerLow = Color(0xFFF8F4F0),
    surfaceContainerHigh = Color(0xFFEFE9E3),
    outline = Color(0xFF8C8178),
    outlineVariant = Color(0xFFDDD3CA),
    error = Color(0xFFB3261E),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFE8917B),
    onPrimary = Color(0xFF4A1709),
    primaryContainer = Color(0xFF6B2C1D),
    onPrimaryContainer = Color(0xFFF6D3C9),
    secondary = Color(0xFFCFC4BA),
    onSecondary = Color(0xFF362E27),
    secondaryContainer = Color(0xFF4A413A),
    onSecondaryContainer = Color(0xFFEDE0D6),
    background = Color(0xFF1B1D21),
    onBackground = Color(0xFFE9E3DE),
    surface = Color(0xFF1B1D21),
    onSurface = Color(0xFFE9E3DE),
    surfaceVariant = Color(0xFF2A2D33),
    onSurfaceVariant = Color(0xFFC9C1B9),
    surfaceContainer = Color(0xFF23262B),
    surfaceContainerLow = Color(0xFF1F2226),
    surfaceContainerHigh = Color(0xFF2B2E34),
    outline = Color(0xFF948A82),
    outlineVariant = Color(0xFF443F3A),
    error = Color(0xFFF2B8B5),
)

/** Tema de la app: claro u oscuro según el móvil, siempre con el color de la marca. */
@Composable
fun TemaMesaLista(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro, content = contenido)
}
