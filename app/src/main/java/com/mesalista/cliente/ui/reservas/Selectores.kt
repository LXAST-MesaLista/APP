package com.mesalista.cliente.ui.reservas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mesalista.cliente.datos.Config
import com.mesalista.cliente.datos.Formato
import com.mesalista.cliente.ui.comun.Aviso
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Tira de los próximos 14 días y «Otra fecha» con un calendario hasta el último día permitido. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorDia(cfg: Config, fecha: LocalDate, alElegir: (LocalDate) -> Unit) {
    val dias = remember(cfg.hoy) { (0..minOf(13, cfg.diasAntelacion)).map { cfg.hoy.plusDays(it.toLong()) } }
    var calendario by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dias) { d ->
                val elegido = d == fecha
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (elegido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (elegido) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    border = if (elegido) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .widthIn(min = 60.dp)
                        .selectable(selected = elegido, role = Role.RadioButton, onClick = { alElegir(d) })
                        .semantics { contentDescription = Formato.fechaLarga(d) },
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Formato.diaRelativo(d, cfg.hoy), style = MaterialTheme.typography.labelSmall)
                        Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Formato.fechaLargaMayuscula(fecha), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { calendario = true }) { Text("Otra fecha") }
        }
    }
    if (calendario) {
        val desde = cfg.hoy.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val hasta = cfg.ultimoDia.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = cfg.hoy.year..cfg.ultimoDia.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in desde..hasta
            },
        )
        DatePickerDialog(
            onDismissRequest = { calendario = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { alElegir(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    calendario = false
                }) { Text("Elegir") }
            },
            dismissButton = { TextButton(onClick = { calendario = false }) { Text("Cancelar") } },
        ) { DatePicker(estado) }
    }
}

/** Comensales con «−» y «+», entre el mínimo y el máximo. */
@Composable
fun SelectorPersonas(valor: Int, minimo: Int, maximo: Int, alCambiar: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FilledTonalIconButton(onClick = { alCambiar(valor - 1) }, enabled = valor > minimo,
            modifier = Modifier.semantics { contentDescription = "Una persona menos" }) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(Formato.personas(valor), style = MaterialTheme.typography.titleMedium)
        FilledTonalIconButton(onClick = { alCambiar(valor + 1) }, enabled = valor < maximo,
            modifier = Modifier.semantics { contentDescription = "Una persona más" }) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** Horas libres, separadas en comidas y cenas. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorHora(horas: List<String>, elegida: String?, alElegir: (String) -> Unit) {
    val turnos = listOf("Comidas" to horas.filter { it < "17:00" }, "Cenas" to horas.filter { it >= "17:00" })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        turnos.filter { it.second.isNotEmpty() }.forEach { (nombre, lista) ->
            Text(nombre, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                lista.forEach { h ->
                    FilterChip(selected = h == elegida, onClick = { alElegir(h) }, label = { Text(h, style = MaterialTheme.typography.titleSmall) })
                }
            }
        }
    }
}

/** Opciones de menú, nota y consentimiento (RP-04), igual al reservar y al modificar. */
class Extras(opciones: Set<Int> = emptySet(), nota: String = "", consentimiento: Boolean = false) {
    var opciones by mutableStateOf(opciones)
    var nota by mutableStateOf(nota)
    var consentimiento by mutableStateOf(consentimiento)

    /** Hay datos que, sin consentimiento, no se guardarán. */
    val sinPermiso: Boolean get() = !consentimiento && (opciones.isNotEmpty() || nota.isNotBlank())

    fun json(destino: org.json.JSONObject): org.json.JSONObject = destino
        .put("menu_options", org.json.JSONArray(opciones.toList()))
        .put("note", nota.trim().ifEmpty { org.json.JSONObject.NULL })
        .put("consent", consentimiento)
}

@Composable
fun FormularioExtras(cfg: Config, extras: Extras, error: String? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Para preparar tu visita (opcional)", style = MaterialTheme.typography.titleMedium)
        Text("Si alguien tiene alergias o necesitáis algo especial, díselo al restaurante.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        cfg.opcionesMenu.forEach { o ->
            val marcada = o.id in extras.opciones
            Row(
                Modifier.fillMaxWidth().toggleable(marcada, role = Role.Checkbox) {
                    extras.opciones = if (it) extras.opciones + o.id else extras.opciones - o.id
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = marcada, onCheckedChange = null)
                Text(o.nombre, Modifier.padding(start = 8.dp))
            }
        }
        OutlinedTextField(
            value = extras.nota,
            onValueChange = { if (it.length <= 200) extras.nota = it },
            label = { Text("Nota para el restaurante") },
            placeholder = { Text("Por ejemplo: venimos con un carrito de bebé") },
            isError = error != null,
            supportingText = { Text(error ?: "${extras.nota.length}/200") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth().toggleable(extras.consentimiento, role = Role.Checkbox) { extras.consentimiento = it },
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(checked = extras.consentimiento, onCheckedChange = null, modifier = Modifier.size(40.dp))
            Text(
                "Acepto que el restaurante use estas opciones y la nota, que pueden revelar datos de salud, " +
                    "solo para preparar mi visita. Se borran 7 días después de la reserva.",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
        if (extras.sinPermiso) {
            Aviso("Sin tu consentimiento, la reserva se guardará sin opciones de menú ni nota.", "aviso")
        }
    }
}
