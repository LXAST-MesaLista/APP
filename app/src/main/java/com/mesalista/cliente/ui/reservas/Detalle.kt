package com.mesalista.cliente.ui.reservas

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.mesalista.cliente.App
import com.mesalista.cliente.datos.ApiError
import com.mesalista.cliente.datos.Config
import com.mesalista.cliente.datos.Datos
import com.mesalista.cliente.datos.Formato
import com.mesalista.cliente.datos.Reserva
import com.mesalista.cliente.ui.comun.Aviso
import com.mesalista.cliente.ui.comun.BotonPrincipal
import com.mesalista.cliente.ui.comun.BotonSecundario
import com.mesalista.cliente.ui.comun.EstadoChip
import com.mesalista.cliente.ui.comun.Mensajes
import com.mesalista.cliente.ui.comun.Pagina
import com.mesalista.cliente.ui.comun.Titulo
import kotlinx.coroutines.launch

/** Detalle de una reserva con su QR (RF-18) y las acciones que permite. */
class DetalleVM(estado: SavedStateHandle) : ViewModel() {
    val id: Int = checkNotNull(estado.get<Int>("id"))
    var reserva by mutableStateOf<Reserva?>(null)
        private set
    var cfg by mutableStateOf<Config?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var cancelando by mutableStateOf(false)
        private set

    /** Siempre se pide otra vez: si la reserva cambia, el QR cambia. */
    fun cargar() {
        viewModelScope.launch {
            error = null
            try {
                cfg = Datos.config()
                reserva = Reserva.de(App.api.get("/reservations/$id").getJSONObject("reservation"))
            } catch (e: ApiError) {
                error = e.texto
            }
        }
    }

    fun cancelar() {
        viewModelScope.launch {
            cancelando = true
            try {
                reserva = Reserva.de(App.api.post("/reservations/$id/cancel").getJSONObject("reservation"))
                Mensajes.mostrar("Reserva cancelada. Te hemos enviado un correo.")
            } catch (e: ApiError) {
                error = e.texto
            } finally {
                cancelando = false
            }
        }
    }
}

@Composable
fun DetallePantalla(irAModificar: (Int) -> Unit, vm: DetalleVM = viewModel()) {
    LaunchedEffect(Unit) { vm.cargar() }
    val r = vm.reserva
    var confirmarCancelar by remember { mutableStateOf(false) }
    Pagina {
        vm.error?.let { Aviso(it) }
        if (r == null) {
            if (vm.error == null) CircularProgressIndicator() else BotonSecundario("Reintentar") { vm.cargar() }
            return@Pagina
        }
        Titulo(Formato.fechaLargaMayuscula(r.fecha))
        EstadoChip(r.estado)

        BloqueSenal(r)

        r.qr?.let { qr ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CodigoQr(qr, Modifier.widthIn(max = 280.dp).fillMaxWidth())
                    Text("Enseña este QR al llegar al restaurante.", style = MaterialTheme.typography.bodyLarge)
                    if (r.ventanaLlegada.size == 2) {
                        Text("Se acepta de ${r.ventanaLlegada[0]} a ${r.ventanaLlegada[1]}.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        val opciones = vm.cfg?.opcionesMenu.orEmpty().filter { it.id in r.opcionesMenu }.map { it.nombre }
        Fila("Hora", "${r.hora} – ${r.horaFin}")
        Fila("Comensales", Formato.personas(r.personas))
        Fila(if (r.mesas.size > 1) "Mesas" else "Mesa", "${r.mesas.joinToString(" + ")} · ${r.zona}")
        if (opciones.isNotEmpty()) Fila("Opciones", opciones.joinToString(", "))
        r.nota?.let { Fila("Nota", it) }

        if (r.estado == "confirmed") {
            Text(
                when {
                    !r.modificable -> "Ya ha pasado el plazo: no se puede modificar ni cancelar desde la app. Si lo necesitas, llama al restaurante."
                    r.grupo -> "Puedes cancelarla hasta el ${Formato.fechaHora(r.plazo)}. Para cambiar la hora o los comensales de un grupo, llama al restaurante."
                    else -> "Puedes modificarla o cancelarla hasta el ${Formato.fechaHora(r.plazo)}."
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (r.modificable) {
            BotonPrincipal(if (r.grupo) "Cambiar menú o nota" else "Modificar", cargando = false) { irAModificar(r.id) }
        }
        if (r.modificable || r.estado == "pending_payment") {
            BotonSecundario("Cancelar reserva", activo = !vm.cancelando) { confirmarCancelar = true }
        }
    }

    if (confirmarCancelar && r != null) {
        val pagada = r.senal?.pago == "captured"
        AlertDialog(
            onDismissRequest = { confirmarCancelar = false },
            title = { Text("¿Cancelar la reserva?") },
            text = {
                Text("La del ${Formato.fechaLarga(r.fecha)} a las ${r.hora}." + if (pagada) " Te devolveremos la señal de ${r.senal?.importe}." else "")
            },
            confirmButton = {
                TextButton(onClick = { confirmarCancelar = false; vm.cancelar() }) { Text("Sí, cancelar") }
            },
            dismissButton = { TextButton(onClick = { confirmarCancelar = false }) { Text("No") } },
        )
    }
}

/** Estado de la señal de un grupo (el pago con PayPal llega en la fase siguiente). */
@Composable
private fun BloqueSenal(r: Reserva) {
    val s = r.senal ?: return
    when {
        r.estado == "pending_payment" && r.pagable -> Aviso("Falta pagar la señal de ${s.importe} para confirmar la reserva.", "aviso")
        r.estado == "pending_payment" -> Aviso("El plazo para pagar la señal ha terminado. La reserva se liberará en unos segundos.", "aviso")
        s.pago == "captured" -> Aviso("Señal pagada: ${s.importe}.", "ok")
        s.pago == "refund_pending" -> Aviso("Señal de ${s.importe}: devolución en curso.", "ok")
        s.pago == "refunded" -> Aviso("Señal de ${s.importe} devuelta a tu cuenta de PayPal.", "ok")
    }
}

@Composable
private fun Fila(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(etiqueta, Modifier.weight(0.38f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, Modifier.weight(0.62f))
    }
}

/**
 * QR de la reserva dibujado en la app con ZXing, siempre en negro sobre
 * blanco. Mientras se ve, la pantalla se pone al máximo de brillo para que
 * el lector lo lea mejor.
 */
@Composable
fun CodigoQr(texto: String, modifier: Modifier = Modifier) {
    val matriz = remember(texto) {
        QRCodeWriter().encode(
            texto, BarcodeFormat.QR_CODE, 0, 0,
            mapOf(EncodeHintType.MARGIN to 2, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
        )
    }
    val actividad = LocalContext.current as? Activity
    DisposableEffect(actividad) {
        val ventana = actividad?.window
        val antes = ventana?.attributes?.screenBrightness
        ventana?.attributes = ventana?.attributes?.apply { screenBrightness = 1f }
        onDispose { ventana?.attributes = ventana?.attributes?.apply { screenBrightness = antes ?: -1f } }
    }
    Canvas(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .semantics { contentDescription = "Código QR de la reserva" },
    ) {
        val n = matriz.width
        val celda = size.width / n
        for (y in 0 until n) for (x in 0 until n) {
            if (matriz.get(x, y)) drawRect(Color.Black, Offset(x * celda, y * celda), Size(celda + 0.5f, celda + 0.5f))
        }
    }
}
