package com.mesalista.cliente.datos

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Fechas, cantidades y estados en español, para las pantallas. */
object Formato {
    private val DIAS = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
    private val MESES = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
        "septiembre", "octubre", "noviembre", "diciembre")

    /** «sábado 7 de noviembre» */
    fun fechaLarga(d: LocalDate) = "${DIAS[d.dayOfWeek.value - 1]} ${d.dayOfMonth} de ${MESES[d.monthValue - 1]}"

    /** «Sábado 7 de noviembre» */
    fun fechaLargaMayuscula(d: LocalDate) = fechaLarga(d).replaceFirstChar { it.uppercase() }

    /** «sáb 7 nov» */
    fun fechaCorta(d: LocalDate) = "${DIAS[d.dayOfWeek.value - 1].take(3)} ${d.dayOfMonth} ${MESES[d.monthValue - 1].take(3)}"

    /** «Hoy», «Mañana» o «lun», para la tira de días. */
    fun diaRelativo(d: LocalDate, hoy: LocalDate) = when (ChronoUnit.DAYS.between(hoy, d)) {
        0L -> "Hoy"
        1L -> "Mañana"
        else -> DIAS[d.dayOfWeek.value - 1].take(3)
    }

    /** «2026-11-07 21:30» → «sábado 7 de noviembre a las 21:30» */
    fun fechaHora(isoHora: String): String {
        if (isoHora.length < 16) return isoHora
        return "${fechaLarga(LocalDate.parse(isoHora.take(10)))} a las ${isoHora.substring(11, 16)}"
    }

    fun personas(n: Int) = if (n == 1) "1 persona" else "$n personas"

    /** Estado de una reserva en pantalla. */
    fun estado(e: String) = when (e) {
        "confirmed" -> "Confirmada"
        "seated" -> "En curso"
        "completed" -> "Completada"
        "cancelled" -> "Cancelada"
        "no_show" -> "No presentada"
        "pending_payment" -> "Pendiente de pago"
        else -> e
    }

    /** Estado de una solicitud de grupo. */
    fun estadoSolicitud(e: String) = when (e) {
        "pending" -> "Esperando respuesta"
        "accepted" -> "Aceptada"
        "rejected" -> "No aceptada"
        "expired" -> "Caducada"
        "cancelled" -> "Cancelada"
        else -> e
    }
}
