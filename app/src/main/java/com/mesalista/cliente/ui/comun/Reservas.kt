package com.mesalista.cliente.ui.comun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mesalista.cliente.datos.Formato
import com.mesalista.cliente.datos.Reserva
import kotlinx.coroutines.flow.MutableSharedFlow

/** Mensajes breves para la barra inferior («Reserva cancelada…»), desde cualquier pantalla. */
object Mensajes {
    val flujo = MutableSharedFlow<String>(extraBufferCapacity = 4)
    fun mostrar(texto: String) {
        flujo.tryEmit(texto)
    }
}

/** Colores (fondo, texto) de cada estado de reserva; el texto siempre acompaña al color. */
@Composable
private fun coloresEstado(estado: String): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (estado) {
        "confirmed", "accepted" -> Color(0xFF1F7A4A) to Color.White
        "seated" -> c.primary to c.onPrimary
        "pending_payment", "pending" -> Color(0xFFF2C14E) to Color(0xFF3A2A00)
        "no_show", "rejected" -> c.error to c.onError
        else -> c.surfaceVariant to c.onSurfaceVariant
    }
}

/** Etiqueta con el estado de una reserva o de una solicitud de grupo. */
@Composable
fun EstadoChip(estado: String, texto: String = Formato.estado(estado)) {
    val (fondo, tinta) = coloresEstado(estado)
    Surface(color = fondo, contentColor = tinta, shape = RoundedCornerShape(50)) {
        Text(texto, Modifier.padding(horizontal = 10.dp, vertical = 3.dp), style = MaterialTheme.typography.labelMedium)
    }
}

/** Una reserva en una lista: fecha y hora, comensales y estado. */
@Composable
fun TarjetaReserva(r: Reserva, alPulsar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Ver la reserva", onClick = alPulsar),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.widthIn(min = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Formato.fechaCorta(r.fecha), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(r.hora, style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(Formato.personas(r.personas), style = MaterialTheme.typography.bodyLarge)
                EstadoChip(r.estado)
            }
            Text(
                when (r.estado) { "confirmed" -> "QR ›"; "pending_payment" -> "Pagar ›"; else -> "›" },
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Subtítulo de sección. */
@Composable
fun Seccion(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}
