package com.mesalista.cliente.ui.comun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/** Página con desplazamiento, márgenes y ancho máximo (en tabletas no se estira). */
@Composable
fun Pagina(modifier: Modifier = Modifier, contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp), content = contenido)
    }
}

/** Campo de formulario con su etiqueta, ayuda y error (el del servidor, si lo hay). */
@Composable
fun Campo(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    clave: Boolean = false,
    ultimo: Boolean = false,
    alTerminar: () -> Unit = {},
    maximo: Int = 254,
    lineas: Int = 1,
    mayusculas: Boolean = false,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { if (it.length <= maximo) alCambiar(it) },
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { texto -> { Text(texto) } },
        singleLine = lineas == 1,
        minLines = lineas,
        visualTransformation = if (clave) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        // Mayúsculas desde el teclado (no transformando el texto al escribir, que puede perder letras).
        keyboardOptions = KeyboardOptions(
            capitalization = if (mayusculas) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
            autoCorrectEnabled = !mayusculas && !clave,
            keyboardType = if (clave) KeyboardType.Password else teclado,
            imeAction = if (ultimo) ImeAction.Done else ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onDone = { alTerminar() }),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Botón principal; mientras trabaja muestra una rueda y no se puede pulsar otra vez. */
@Composable
fun BotonPrincipal(texto: String, cargando: Boolean, modifier: Modifier = Modifier, activo: Boolean = true, alPulsar: () -> Unit) {
    Button(onClick = alPulsar, enabled = activo && !cargando, modifier = modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        if (cargando) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(10.dp))
        }
        Text(texto)
    }
}

@Composable
fun BotonSecundario(texto: String, modifier: Modifier = Modifier, activo: Boolean = true, alPulsar: () -> Unit) {
    OutlinedButton(onClick = alPulsar, enabled = activo, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(texto)
    }
}

/** Aviso destacado: «error», «aviso» u «ok». Se anuncia a los lectores de pantalla. */
@Composable
fun Aviso(texto: String, tipo: String = "error", modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val (fondo, tinta) = when (tipo) {
        "ok" -> c.secondaryContainer to c.onSecondaryContainer
        "aviso" -> c.primaryContainer to c.onPrimaryContainer
        else -> c.errorContainer to c.onErrorContainer
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = fondo, contentColor = tinta),
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(texto, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Título de página. */
@Composable
fun Titulo(texto: String, subtitulo: String? = null) {
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Text(texto, style = MaterialTheme.typography.headlineSmall)
        if (subtitulo != null) {
            Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Fila con dos botones a lo ancho. */
@Composable
fun FilaBotones(contenido: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), content = contenido)
}
