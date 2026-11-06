package com.mesalista.cliente.datos

import com.mesalista.cliente.App
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/*
 * Datos que devuelve la API, leídos de su JSON (formato en docs/openapi.yaml
 * del servidor). Solo lo que usan las pantallas.
 */

private fun JSONArray?.enteros(): List<Int> = this?.let { a -> (0 until a.length()).map { a.getInt(it) } } ?: emptyList()
private fun JSONArray?.textos(): List<String> = this?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList()
private fun JSONObject.textoONull(clave: String): String? = if (isNull(clave)) null else optString(clave)

data class OpcionMenu(val id: Int, val nombre: String)

/** GET /config: reglas públicas del restaurante. */
data class Config(
    val minPersonas: Int,
    val maxPersonas: Int,
    val diasAntelacion: Int,
    val duracionMin: Int,
    val opcionesMenu: List<OpcionMenu>,
    val hoy: LocalDate,
    val maxGrupo: Int,
    val senalPorPersona: Int,
    val minutosSenal: Int,
) {
    val ultimoDia: LocalDate get() = hoy.plusDays(diasAntelacion.toLong())

    companion object {
        fun de(j: JSONObject): Config {
            val ops = j.optJSONArray("menu_options") ?: JSONArray()
            return Config(
                minPersonas = j.optInt("min_party", 1),
                maxPersonas = j.optInt("max_party", 10),
                diasAntelacion = j.optInt("max_advance_days", 30),
                duracionMin = j.optInt("duration_min", 120),
                opcionesMenu = (0 until ops.length()).map { ops.getJSONObject(it) }.map { OpcionMenu(it.getInt("id"), it.getString("name")) },
                hoy = LocalDate.parse(j.getString("today")),
                maxGrupo = j.optInt("max_group_party", 40),
                senalPorPersona = j.optInt("deposit_per_person", 0),
                minutosSenal = j.optInt("deposit_hold_min", 15),
            )
        }
    }
}

/** Señal de una reserva de grupo (RF-44). payment: null, captured, refund_pending o refunded. */
data class Senal(val importe: String, val vence: String?, val segundosRestantes: Long?, val pago: String?) {
    companion object {
        fun de(j: JSONObject?) = j?.let {
            Senal(it.optString("amount"), it.textoONull("due"), if (it.isNull("seconds_left")) null else it.optLong("seconds_left"), it.textoONull("payment"))
        }
    }
}

/** Una reserva del cliente (GET /reservations y /reservations/{id}). */
data class Reserva(
    val id: Int,
    val estado: String,
    val fecha: LocalDate,
    val hora: String,
    val horaFin: String,
    val personas: Int,
    val mesas: List<String>,
    val zona: String,
    val opcionesMenu: List<Int>,
    val nota: String?,
    val senal: Senal?,
    val modificable: Boolean,
    val plazo: String,
    val ventanaLlegada: List<String>,
    val qr: String?,
    val grupo: Boolean,
    val pagable: Boolean,
) {
    /** Pendiente: aún por venir o en curso. */
    val pendiente: Boolean get() = estado in setOf("confirmed", "seated", "pending_payment")

    companion object {
        fun de(j: JSONObject) = Reserva(
            id = j.getInt("id"),
            estado = j.getString("status"),
            fecha = LocalDate.parse(j.getString("date")),
            hora = j.getString("time"),
            horaFin = j.optString("end_time"),
            personas = j.getInt("party"),
            mesas = j.optJSONArray("tables").textos(),
            zona = j.optString("zone"),
            opcionesMenu = j.optJSONArray("menu_options").enteros(),
            nota = j.textoONull("note"),
            senal = Senal.de(j.optJSONObject("deposit")),
            modificable = j.optBoolean("modifiable"),
            plazo = j.optString("deadline"),
            ventanaLlegada = j.optJSONArray("arrival_window").textos(),
            qr = j.textoONull("qr"),
            grupo = j.optBoolean("group"),
            pagable = j.optBoolean("payable"),
        )

        fun lista(j: JSONObject, clave: String = "reservations"): List<Reserva> {
            val a = j.optJSONArray(clave) ?: return emptyList()
            return (0 until a.length()).map { de(a.getJSONObject(it)) }
        }
    }
}

/** Solicitud de mesa para un grupo grande (RF-43). */
data class SolicitudGrupo(
    val id: Int,
    val fecha: LocalDate,
    val hora: String,
    val personas: Int,
    val nota: String?,
    val estado: String,
    val respuesta: String?,
    val reservaId: Int?,
) {
    companion object {
        fun lista(j: JSONObject): List<SolicitudGrupo> {
            val a = j.optJSONArray("requests") ?: return emptyList()
            return (0 until a.length()).map { a.getJSONObject(it) }.map {
                SolicitudGrupo(
                    id = it.getInt("id"),
                    fecha = LocalDate.parse(it.getString("date")),
                    hora = it.getString("time"),
                    personas = it.getInt("party"),
                    nota = it.textoONull("note"),
                    estado = it.getString("status"),
                    respuesta = it.textoONull("reply"),
                    reservaId = if (it.isNull("reservation_id")) null else it.getInt("reservation_id"),
                )
            }
        }
    }
}

/** Aviso del restaurante (RF-32), p. ej. una cancelación. */
data class AvisoRestaurante(val id: Int, val mensaje: String, val reservaId: Int?) {
    companion object {
        fun lista(j: JSONObject): List<AvisoRestaurante> {
            val a = j.optJSONArray("notifications") ?: return emptyList()
            return (0 until a.length()).map { a.getJSONObject(it) }.map {
                AvisoRestaurante(it.getInt("id"), it.getString("message"), if (it.isNull("reservation_id")) null else it.getInt("reservation_id"))
            }
        }
    }
}

/** Datos que se piden una vez y comparten las pantallas. */
object Datos {
    @Volatile
    private var config: Config? = null

    /** Configuración del restaurante (se vuelve a pedir cada día o si se fuerza). */
    suspend fun config(forzar: Boolean = false): Config {
        config?.let { if (!forzar && it.hoy == LocalDate.now()) return it }
        return Config.de(App.api.get("/config")).also { config = it }
    }
}
