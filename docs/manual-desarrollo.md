# Manual: hacer una app Android con Kotlin, Jetpack Compose y Firebase para MesaLista

Este manual explica, desde cero, cómo está hecha la app de MesaLista y cómo
hacer una igual o cambiarla: estructura del programa, conexión con la API del
servidor (detrás de nginx), todas las rutas, cómo circula la información, cómo
se muestran los datos y cómo se configura Firebase.

No hace falta saber nada de antemano. Cada término nuevo se explica la primera
vez que sale, y hay un glosario al final.

Documentos relacionados:

- [pantallas.md](pantallas.md): qué archivo dibuja cada pantalla.
- [manual-estilo.md](manual-estilo.md): colores, letra, textos e icono.
- Contrato completo de la API: `docs/openapi.yaml` y `docs/integracion.md` del
  repositorio [LXPT-UIB/Servidor](https://github.com/LXPT-UIB/Servidor).

---

## 1. Qué es cada pieza

```
 Móvil (app Kotlin)
     │  HTTPS · JSON · token Bearer
     ▼
 nginx  (https://mesalista.spaincentral.cloudapp.azure.com)
     │  /api/v1/…  →  127.0.0.1:8091
     ▼
 API de MesaLista (Node.js)  ──►  PostgreSQL (reservas, cuentas…)
     │
     ├──►  Firebase Cloud Messaging  ──►  aviso push en el móvil
     └──►  PayPal sandbox (pago de la señal de los grupos)
```

| Pieza | Qué hace | Dónde está |
|---|---|---|
| **App** | Muestra pantallas, recoge lo que hace el usuario y llama a la API | Este repositorio (LXPT-UIB/APP) |
| **nginx** | Recibe todo por HTTPS en el dominio y pasa `/api/v1` a la API | Máquina de Azure |
| **API** | Todas las reglas: horas libres, mesas, plazos, QR, pagos, cuentas | LXPT-UIB/Servidor |
| **PostgreSQL** | Base de datos. La app **nunca** se conecta a ella | Máquina de Azure, solo en local |
| **Firebase (FCM)** | Hace llegar avisos push al móvil | Consola de Firebase, proyecto `mesalista-57287` |
| **PayPal** | Cobra la señal de las reservas de grupo | developer.paypal.com (sandbox) |

### La regla principal

**Todas las reglas están en el servidor.** La app no decide si una hora está
libre, qué mesa toca ni si un plazo ha pasado: pregunta a la API y muestra lo
que responde. Puede ayudar al usuario (por ejemplo, no dejar enviar un campo
vacío), pero la última palabra es siempre de la API. Así la web y la app se
comportan igual, y nadie puede saltarse una regla modificando la app.

---

## 2. Herramientas

| Herramienta | Para qué | Dónde |
|---|---|---|
| **Android Studio** (última versión estable) | Editor, compilador, emulador | developer.android.com/studio |
| **JDK 17 o 21** | Compilar Kotlin y Gradle | Viene dentro de Android Studio («jbr») |
| **Android SDK 35** | Las librerías de Android 15 | Android Studio > *SDK Manager* |
| **Emulador** o móvil con *Depuración USB* | Ejecutar la app | *Device Manager* |
| **Git** | Bajar y subir el código | git-scm.com |

Para abrir este proyecto: *File > Open* y elegir la carpeta raíz (la que tiene
`settings.gradle.kts`). La primera sincronización de Gradle descarga todo y
tarda unos minutos.

### Empezar un proyecto desde cero

1. *File > New > New Project > Empty Activity* (es la plantilla de Compose).
2. *Name*: `MesaLista`. *Package name*: `com.mesalista.cliente`. Tiene que ser el
   mismo que la app registrada en Firebase.
3. *Minimum SDK*: API 26 (Android 8.0). *Build configuration language*: Kotlin DSL.
4. Pulsa *Finish* y copia la estructura del apartado 3.

---

## 3. Cómo se estructura el programa

### 3.1 Carpetas

```
APP/
├── build.gradle.kts            versiones de los plugins (Android, Kotlin, Compose, Google Services)
├── settings.gradle.kts         nombre del proyecto y repositorios de librerías
├── gradle.properties           dirección de la API (mesalista.api y mesalista.api.debug)
├── keystore.properties, keystore/   clave de firma (no se suben a Git)
└── app/
    ├── build.gradle.kts        versión de la app, librerías, firma, BuildConfig.API
    ├── google-services.json    configuración de Firebase (no se sube a Git)
    └── src/
        ├── debug/res/xml/network_security_config.xml   permite HTTP solo hacia 10.0.2.2
        └── main/
            ├── AndroidManifest.xml     permisos, actividad, enlaces mesalista://, servicio de Firebase
            ├── res/                    icono, colores de arranque, nombre, seguridad de red
            └── java/com/mesalista/cliente/
                ├── MesaListaApp.kt     arranque: crea la sesión y el cliente de la API
                ├── MainActivity.kt     la única actividad: pone Compose y recibe enlaces
                ├── AvisosServicio.kt   servicio de Firebase (token nuevo, aviso con la app abierta)
                ├── datos/              TODO lo que no es pantalla
                │   ├── Api.kt          llamadas HTTP a la API (OkHttp) y ApiError
                │   ├── Sesion.kt       token cifrado y usuario actual
                │   ├── Modelos.kt      clases de datos (Reserva, Config…) leídas del JSON
                │   ├── Formato.kt      fechas y estados en español
                │   ├── Avisos.kt       Firebase: permiso, canal, registro del móvil
                │   ├── Enlaces.kt      vuelta de PayPal y avisos tocados
                │   └── Acciones.kt     acciones compartidas (cerrar sesión)
                └── ui/                 las pantallas (Compose)
                    ├── Navegacion.kt   rutas, barra superior e inferior
                    ├── tema/Tema.kt    colores y tema claro/oscuro
                    ├── comun/          piezas reutilizables (botones, campos, tarjetas)
                    ├── acceso/         bienvenida, entrar, registro, recuperar
                    ├── inicio/         inicio
                    ├── reservas/       reservar, confirmar, mis reservas, detalle, modificar, grupos, vincular
                    └── cuenta/         mi cuenta
```

La idea es separar en **capas**:

| Capa | Carpeta | Regla |
|---|---|---|
| **Datos** | `datos/` | Habla con la API, guarda la sesión y convierte JSON en clases. No dibuja nada. |
| **Estado de cada pantalla** | La clase `…VM` de cada archivo de `ui/` | Pide los datos a la capa de datos y guarda lo que la pantalla necesita (cargando, error, lista…). |
| **Dibujo** | La función `@Composable …Pantalla` | Solo muestra el estado y avisa al `…VM` cuando el usuario pulsa algo. |

### 3.2 Una pantalla por dentro: ViewModel + Composable

Cada archivo de pantalla tiene dos partes. Ejemplo simplificado de
`ui/inicio/Inicio.kt`:

```kotlin
/** Estado de la pantalla: sobrevive a giros del móvil. */
class InicioVM : ViewModel() {
    var cargando by mutableStateOf(true)          // Compose redibuja al cambiar
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var proximas by mutableStateOf<List<Reserva>>(emptyList())
        private set

    fun cargar() {
        viewModelScope.launch {                   // corrutina: no bloquea la pantalla
            try {
                val r = App.api.get("/reservations")
                proximas = Reserva.lista(r).filter { it.pendiente }
            } catch (e: ApiError) {
                error = e.texto                    // mensaje del servidor, en español
            } finally {
                cargando = false
            }
        }
    }
}

@Composable
fun InicioPantalla(irAReserva: (Int) -> Unit, vm: InicioVM = viewModel()) {
    LaunchedEffect(Unit) { vm.cargar() }          // se ejecuta al abrir la pantalla
    Pagina {
        Titulo("Próximas reservas")
        vm.error?.let { Aviso(it) }
        when {
            vm.cargando -> CircularProgressIndicator()
            vm.proximas.isEmpty() -> Text("No tienes ninguna reserva pendiente.")
            else -> vm.proximas.forEach { r -> TarjetaReserva(r) { irAReserva(r.id) } }
        }
    }
}
```

- **ViewModel**: objeto que guarda el estado de una pantalla. No se pierde
  al girar el móvil y se borra al salir de la pantalla.
- **`mutableStateOf`**: una variable que Compose vigila. Cuando cambia, la
  pantalla se vuelve a dibujar sola.
- **Corrutina** (`viewModelScope.launch`): una tarea que espera a la red sin
  congelar la pantalla.
- **Composable** (`@Composable`): una función que describe cómo se ve una parte
  de la pantalla a partir de los datos. No guarda nada.
- Los parámetros como `irAReserva` son **acciones de navegación**: la pantalla
  no sabe a dónde lleva, se lo dice `Navegacion.kt`.

### 3.3 Objetos compartidos

`MesaListaApp.kt` crea **una sola vez**, al abrir la app, lo que usan todas las
pantallas:

```kotlin
class MesaListaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        App.sesion = Sesion(this)                    // token y usuario
        App.api = Api(BuildConfig.API, App.sesion)   // cliente de la API
        if (Avisos.activo) Avisos.crearCanal(this)   // Firebase
    }
}

object App {
    lateinit var sesion: Sesion
    lateinit var api: Api
}
```

Cualquier pantalla puede hacer `App.api.get("/config")`.

---

## 4. Configuración del proyecto (Gradle)

### 4.1 Plugins (`build.gradle.kts` de la raíz)

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false   // Firebase
}
```

### 4.2 Librerías (`app/build.gradle.kts`)

| Librería | Para qué |
|---|---|
| `androidx.compose:compose-bom` + `ui`, `material3`, `material-icons-core` | Pantallas con Compose y Material 3 |
| `androidx.activity:activity-compose` | Poner Compose dentro de la actividad |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `viewModel()` en las pantallas |
| `androidx.navigation:navigation-compose` | Ir de una pantalla a otra |
| `androidx.core:core-splashscreen` | Pantalla de carga |
| `com.squareup.okhttp3:okhttp` | Peticiones HTTPS a la API |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | Corrutinas |
| `com.google.zxing:core` | Dibujar el QR de la reserva |
| `androidx.browser:browser` | Abrir PayPal en una pestaña del navegador (Custom Tabs) |
| `com.google.firebase:firebase-bom` + `firebase-messaging` | Avisos push |

El JSON se lee con `org.json`, que ya viene en Android y no necesita librería.

### 4.3 La dirección de la API: `gradle.properties` y `BuildConfig`

La dirección **no se escribe en el código**. Va en `gradle.properties`:

```properties
# Versión que se instala en los móviles (release): siempre HTTPS.
mesalista.api=https://mesalista.spaincentral.cloudapp.azure.com/api/v1
# Versión de desarrollo (debug): la API de este ordenador vista desde el emulador.
mesalista.api.debug=http://10.0.2.2:8091/api/v1
```

`app/build.gradle.kts` la convierte en la constante `BuildConfig.API`:

```kotlin
buildTypes {
    debug {
        val api = providers.gradleProperty("mesalista.api.debug").get().trimEnd('/')
        buildConfigField("String", "API", "\"$api\"")
    }
    release {
        val api = providers.gradleProperty("mesalista.api").get().trimEnd('/')
        require(api.startsWith("https://")) { "mesalista.api debe empezar por https://" }
        buildConfigField("String", "API", "\"$api\"")
        isMinifyEnabled = true
    }
}
buildFeatures { compose = true; buildConfig = true }
```

- **Sin servidor en tu PC:** pon también en `mesalista.api.debug` la dirección
  de Azure (`https://…/api/v1`) y pulsa *Sync Now*.
- **Con la API en tu PC** (`node src/server.js` del Servidor, con
  `"require_https": false` en su `config.json`): deja `http://10.0.2.2:8091/api/v1`.
  Para el emulador, `10.0.2.2` es tu ordenador. En un móvil real no funciona:
  usa la IP de tu PC en la red wifi o la de Azure.

### 4.4 Seguridad de red y permisos

`res/xml/network_security_config.xml` (versión release): solo HTTPS y solo
certificados del sistema.

```xml
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors><certificates src="system" /></trust-anchors>
    </base-config>
</network-security-config>
```

`src/debug/res/xml/network_security_config.xml` añade una excepción **solo en debug**:
HTTP permitido hacia `10.0.2.2`.

`AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />   <!-- avisos, Android 13+ -->
<application android:name=".MesaListaApp" android:allowBackup="false"
             android:networkSecurityConfig="@xml/network_security_config" …>
```

---

## 5. Conectar con la API (detrás de nginx)

nginx recibe las peticiones en `https://<dominio>/api/v1/…` y las pasa a la API.
Para la app, la API es simplemente esa dirección base. Todas las rutas del
apartado 7 se escriben **después** de `/api/v1`, por ejemplo `BuildConfig.API + "/config"`.

### 5.1 El cliente: `datos/Api.kt`

Una sola clase hace todas las peticiones:

```kotlin
class Api(private val base: String, private val sesion: Sesion) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val json = "application/json; charset=utf-8".toMediaType()

    suspend fun get(ruta: String) = llamar("GET", ruta, null)
    suspend fun post(ruta: String, cuerpo: JSONObject = JSONObject()) = llamar("POST", ruta, cuerpo)
    suspend fun patch(ruta: String, cuerpo: JSONObject) = llamar("PATCH", ruta, cuerpo)
    suspend fun delete(ruta: String, cuerpo: JSONObject? = null) = llamar("DELETE", ruta, cuerpo)

    suspend fun llamar(metodo: String, ruta: String, cuerpo: JSONObject?): JSONObject =
        withContext(Dispatchers.IO) {                       // la red nunca en el hilo de la pantalla
            val token = sesion.token
            val peticion = Request.Builder()
                .url(base + ruta)
                .header("Accept", "application/json")
                .apply { if (token != null) header("Authorization", "Bearer $token") }
                .method(metodo, cuerpo?.toString()?.toRequestBody(json))
                .build()
            val respuesta = try {
                http.newCall(peticion).execute()
            } catch (e: IOException) {
                throw ApiError(0, "network", "No se puede conectar con el servidor.", null)
            }
            respuesta.use { res ->
                val datos = runCatching { JSONObject(res.body?.string().orEmpty()) }.getOrDefault(JSONObject())
                if (res.isSuccessful) return@withContext datos
                if (res.code == 401 && token != null) sesion.caducada()   // a «Entrar»
                val e = datos.optJSONObject("error") ?: JSONObject()
                throw ApiError(res.code, e.optString("code"), e.optString("message"), e.optString("field"))
            }
        }
}
```

Lo importante:

1. **Una petición es método + ruta + (cuerpo JSON) + cabeceras.**
2. Con sesión abierta, cada petición lleva `Authorization: Bearer <token>`.
3. La respuesta siempre es JSON. Si va bien (`2xx`), se devuelve el objeto.
4. Si va mal, se lanza un `ApiError` con el `code` y el `message` del servidor.
   Las pantallas lo capturan con `catch (e: ApiError)` y muestran `e.texto`.
5. Un `401` con token significa **sesión caducada**: se borra y la app vuelve a «Entrar».
6. Sin red, el error tiene `status = 0` y `code = "network"`.

### 5.2 Llamar desde una pantalla

```kotlin
viewModelScope.launch {
    try {
        val r = App.api.get("/availability?date=2026-11-28&party=4")
        horas = r.getJSONArray("times").let { a -> (0 until a.length()).map { a.getString(it) } }
    } catch (e: ApiError) {
        error = e.texto
    }
}
```

Los parámetros de un `GET` van en la ruta (`?date=…&party=…`). Los de `POST`,
`PATCH` y `DELETE` van en un `JSONObject`.

---

## 6. Cómo circula la información (API REST)

### 6.1 Qué es REST

- Cada **ruta** es un recurso: `/reservations` son tus reservas y `/reservations/15`
  es una de ellas.
- El **método** dice qué haces con él: `GET` leer, `POST` crear o hacer una
  acción, `PATCH` cambiar una parte, `DELETE` borrar.
- Los datos viajan en **JSON** (texto con `{ "clave": valor }`).
- El **código HTTP** de la respuesta dice cómo ha ido: `200` bien, `201` creado,
  `4xx` error tuyo o regla incumplida, `5xx` error del servidor.

### 6.2 Un viaje completo: iniciar sesión

1. El usuario escribe correo y contraseña y pulsa «Entrar» (`EntrarPantalla`).
2. `EntrarVM` llama a la API:

   ```http
   POST /api/v1/auth/login
   Content-Type: application/json

   {"email": "lucia@mesalista.test", "password": "una-contraseña-larga"}
   ```

3. nginx pasa la petición a la API, que comprueba la contraseña en PostgreSQL.
4. Responde:

   ```json
   {
     "user": {"id": 7, "name": "Lucía Pérez", "email": "lucia@mesalista.test", "phone": "600123123"},
     "session": {"token": "9f2c…(64 caracteres)…", "kind": "app", "expires_at": "2026-11-19T13:00:00+01:00"}
   }
   ```

5. `Sesion.entrar(respuesta)` guarda el token **cifrado** (apartado 13) y el usuario.
6. `Navegacion.kt` ve que ya hay usuario y cambia a las pantallas del cliente.
7. A partir de aquí, cada petición lleva `Authorization: Bearer 9f2c…`.

```
 Pantalla ──► VM ──► Api.llamar ──HTTPS──► nginx ──► API ──► PostgreSQL
    ▲                                                  │
    └──── estado (mutableStateOf) ◄── JSON ◄───────────┘
```

### 6.3 La sesión

- El token de cliente dura **60 minutos**. Cuando caduca, cualquier petición
  responde `401 unauthenticated` y la app vuelve a «Entrar».
- `POST /auth/logout` invalida el token en el servidor. La app además lo borra
  del móvil.
- Tras 5 contraseñas mal, la cuenta queda bloqueada 15 minutos (`429 locked`).

### 6.4 Errores: siempre la misma forma

```json
{"error": {"code": "conflict", "message": "Esa hora ya no está disponible. Elige otra.", "field": "time"}}
```

| HTTP | `code` frecuentes | Qué hace la app |
|---|---|---|
| 0 | `network` | «No se puede conectar»: botón «Reintentar» |
| 400 | `bad_json`, `reset_invalid` | Error del programa, o enlace caducado |
| 401 | `unauthenticated`, `bad_credentials` | Volver a «Entrar», o «correo o contraseña incorrectos» |
| 403 | `forbidden`, `bad_password` | Sin permiso, o contraseña mal (eliminar cuenta) |
| 404 | `not_found` | No existe o no es tuya |
| 409 | `conflict`, `limit`, `deadline`, `email_taken`, `not_cancellable`, `payment_expired`… | Choca con una regla: mostrar `message` |
| 422 | `invalid`, `closed`, `past`, `too_soon`, `too_far` | Dato mal: mostrar `message` **debajo del campo** `field` |
| 429 | `locked` | Demasiados intentos: esperar |
| 503 | `payments_off` | PayPal no disponible |

**Reacciona al `code`, muestra el `message`.** Los mensajes ya vienen en español
y no hay que traducirlos. Ejemplo de `ConfirmarVM`:

```kotlin
catch (e: ApiError) {
    when {
        e.codigo == "conflict" -> { Mensajes.mostrar(e.texto); alVolverAHoras() }  // otra persona se adelantó
        e.campo == "note" -> errorNota = e.texto                                      // error en un campo
        else -> error = e.texto
    }
}
```

### 6.5 No duplicar reservas: `request_id`

Si el móvil pierde la cobertura justo al reservar, el usuario puede pulsar otra
vez. Para que no se creen dos reservas, cada intento de reserva lleva un
`request_id` (un UUID). Si se repite con el mismo, la API devuelve la reserva
ya creada con `"replayed": true` y código `200`, en vez de `201`.

```kotlin
private val requestId = UUID.randomUUID().toString()   // uno por pantalla de confirmar
```

---

## 7. Guía de rutas de la API

Base: `https://mesalista.spaincentral.cloudapp.azure.com/api/v1`.

**Auth**: «—» se puede llamar sin sesión; «cliente» necesita el token Bearer de
`/auth/login` o `/auth/register`.

### 7.1 Públicas

| Método y ruta | Auth | Cuerpo / parámetros | Respuesta | Pantalla |
|---|---|---|---|---|
| `GET /config` | — | — | `min_party`, `max_party`, `max_advance_days`, `duration_min`, `menu_options[{id,name}]`, `today`, `max_group_party`, `deposit_per_person`, `deposit_hold_min` | Reservar, Confirmar, Grupos (`Datos.config()`) |
| `GET /availability?date=AAAA-MM-DD&party=N` | — | En la ruta | `times: ["13:00", "13:30", …]` | Reservar, Modificar |

### 7.2 Cuenta

| Método y ruta | Auth | Cuerpo | Respuesta | Pantalla |
|---|---|---|---|---|
| `POST /auth/register` | — | `name`, `email`, `phone`, `password` (10–128 caracteres, no de las más comunes) | `201` `user`, `session` | Crear cuenta |
| `POST /auth/login` | — | `email`, `password` | `user`, `session` | Entrar |
| `POST /auth/logout` | cliente | — | `ok` | Cuenta > Cerrar sesión |
| `POST /auth/forgot` | — | `email` | `ok` (igual exista o no la cuenta) | He olvidado la contraseña |
| `POST /auth/reset` | — | `token` (del correo), `password` | `ok` | Página de la web `/restablecer` |
| `GET /me` | cliente | — | `user` | Inicio (refresca el nombre) |
| `PATCH /me` | cliente | `name` y/o `phone` | `user` | Mi cuenta |
| `DELETE /me` | cliente | `password` | `ok` | Mi cuenta > Eliminar cuenta |

### 7.3 Reservas

| Método y ruta | Auth | Cuerpo | Respuesta | Pantalla |
|---|---|---|---|---|
| `GET /reservations` | cliente | — | `reservations: [Reserva]` | Inicio, Mis reservas |
| `POST /reservations` | cliente | `request_id`, `date`, `time`, `party`, `menu_options[ids]`, `note`, `consent` | `201` `reservation`, `replayed` | Confirmar |
| `GET /reservations/{id}` | cliente | — | `reservation` | Detalle de la reserva |
| `PATCH /reservations/{id}` | cliente | `date`, `time`, `party`, `menu_options`, `note`, `consent` (los que cambien) | `reservation` | Modificar |
| `POST /reservations/{id}/cancel` | cliente | — | `reservation` (cancelada) | Detalle > Cancelar |
| `POST /reservations/link` | cliente | `code` (del ticket), `phone` | `reservation` | Añadir una reserva del local |
| `POST /reservations/{id}/pay` | cliente | `from: "app"` | `approve_url` (página de PayPal) | Detalle > Pagar con PayPal |
| `GET /payments/return?token=…` | — | La llama PayPal, no la app | Redirige a `mesalista://reserva/{id}?pago=ok\|cancelado\|caducado\|error` | — |

**`note` y `consent`:** la nota (alergias, peticiones) solo se guarda si
`consent` es `true` (protección de datos). Sin consentimiento se ignora.

### 7.4 Solicitudes de grupo

Para más personas que `max_party`, hasta `max_group_party`.

| Método y ruta | Auth | Cuerpo | Respuesta | Pantalla |
|---|---|---|---|---|
| `GET /group-requests/times?date=` | cliente | En la ruta | `times` (horas que se pueden pedir) | Grupo grande |
| `GET /group-requests` | cliente | — | `requests: [Solicitud]` | Mis reservas |
| `POST /group-requests` | cliente | `date`, `time`, `party`, `note`, `consent` | `201` `requests` | Grupo grande |
| `POST /group-requests/{id}/cancel` | cliente | — | `requests` | Mis reservas > Cancelar solicitud |

Ciclo de una solicitud: `pending` (esperando al local) → `accepted` (el local
elige mesas; si hay señal, la reserva queda `pending_payment`) o `rejected`
(con motivo en `reply`). El cliente puede cancelarla mientras está `pending`
(`cancelled`). Si no paga a tiempo, pasa a `expired`.

### 7.5 Avisos

| Método y ruta | Auth | Cuerpo | Respuesta | Uso |
|---|---|---|---|---|
| `POST /me/devices` | cliente | `token` (de Firebase), `platform: "android"` | `201` o `200` | Registrar el móvil para recibir push |
| `DELETE /me/devices` | cliente | `token` | `ok` | Al cerrar sesión |
| `GET /me/notifications` | cliente | — | `notifications [{id, message, reservation_id, created_at}]` | Inicio: avisos del restaurante |
| `POST /me/notifications/read` | cliente | — | `ok` | Inicio > «Entendido» |

### 7.6 Las rutas del panel no son para la app

`/panel/*` y `/admin/*` son del personal del restaurante (sala, agenda,
mesas…) y las usa la web. Un token de cliente recibe `401` o `403` en ellas.

### 7.7 Probar una ruta sin la app

Con `curl` (en cmd, PowerShell o Git Bash):

```bash
curl https://mesalista.spaincentral.cloudapp.azure.com/api/v1/config
curl -X POST https://mesalista.spaincentral.cloudapp.azure.com/api/v1/auth/login -H "Content-Type: application/json" -d "{\"email\":\"lucia@mesalista.test\",\"password\":\"…\"}"
curl https://mesalista.spaincentral.cloudapp.azure.com/api/v1/reservations -H "Authorization: Bearer <token>"
```

También sirven Postman o Insomnia, o abrir `docs/openapi.yaml` del Servidor en
editor.swagger.io para ver todas las rutas con sus campos.

---

## 8. Cómo representar los datos de las respuestas

### 8.1 Del JSON a una clase de Kotlin

Nunca se pasa el `JSONObject` a las pantallas: se convierte antes en una
**data class** con nombres en español y tipos correctos (`datos/Modelos.kt`).

Respuesta de la API (una reserva):

```json
{
  "id": 15, "status": "confirmed", "date": "2026-11-28", "time": "21:00", "end_time": "23:00",
  "party": 4, "table": "S-3", "tables": ["S-3"], "zone": "sala", "menu_options": [2],
  "note": null, "deposit": null, "modifiable": true, "deadline": "2026-11-28 17:00",
  "arrival_window": ["20:45", "21:15"], "qr": "ML1.15.1.a9f…", "group": false, "payable": false
}
```

Clase y lectura:

```kotlin
data class Reserva(
    val id: Int, val estado: String, val fecha: LocalDate, val hora: String,
    val personas: Int, val mesas: List<String>, val nota: String?, val qr: String?,
    val modificable: Boolean, val pagable: Boolean,
    /* … */
) {
    val pendiente: Boolean get() = estado in setOf("confirmed", "seated", "pending_payment")

    companion object {
        fun de(j: JSONObject) = Reserva(
            id = j.getInt("id"),
            estado = j.getString("status"),
            fecha = LocalDate.parse(j.getString("date")),     // texto → fecha de verdad
            hora = j.getString("time"),
            personas = j.getInt("party"),
            mesas = j.optJSONArray("tables").textos(),
            nota = if (j.isNull("note")) null else j.optString("note"),   // null de verdad, no "null"
            qr = if (j.isNull("qr")) null else j.optString("qr"),
            modificable = j.optBoolean("modifiable"),
            pagable = j.optBoolean("payable"),
        )
        fun lista(j: JSONObject) = j.optJSONArray("reservations")
            ?.let { a -> (0 until a.length()).map { de(a.getJSONObject(it)) } } ?: emptyList()
    }
}
```

Reglas:

- **`getX`** para lo que siempre viene (falla si falta). **`optX`** para lo opcional.
- **`null`**: comprueba `isNull` antes. `optString` de un `null` devuelve el texto `"null"`.
- **Fechas**: `LocalDate.parse("2026-11-28")`. Las horas llegan como `"21:00"` y se muestran tal cual.
- **Dinero**: la API ya manda el importe formateado (`deposit.amount` = `"55 €"`) y en céntimos (`deposit.cents`). Muestra `amount`.

### 8.2 Estados: el texto y el color salen del código del estado

La API devuelve códigos en inglés (`confirmed`, `pending_payment`…). La app los
traduce en un único sitio (`datos/Formato.kt`):

| `status` (reserva) | Texto | Color |
|---|---|---|
| `confirmed` | Confirmada | Verde |
| `pending_payment` | Pendiente de pago | Amarillo |
| `seated` | En curso | Color de la marca |
| `completed` | Completada | Gris |
| `cancelled` | Cancelada | Gris |
| `no_show` | No presentada | Rojo |

| `status` (solicitud de grupo) | Texto |
|---|---|
| `pending` | Esperando respuesta |
| `accepted` | Aceptada |
| `rejected` | No aceptada |
| `expired` | Caducada |
| `cancelled` | Cancelada |

El color nunca va solo: siempre acompaña al texto, para quien no distingue colores.

### 8.3 Los cuatro estados de toda pantalla con datos

Toda pantalla que pide algo a la API debe tener en cuenta:

1. **Cargando**: `CircularProgressIndicator()`.
2. **Error**: `Aviso(mensaje)` y un botón «Reintentar».
3. **Vacío**: un texto («No tienes reservas pendientes»).
4. **Con datos**: la lista o el detalle.

### 8.4 Listas

Para pocas tarjetas (como en MesaLista), un `forEach` dentro de `Pagina { … }`.
Para listas largas, `LazyColumn`, que solo dibuja lo que se ve:

```kotlin
LazyColumn { items(reservas, key = { it.id }) { r -> TarjetaReserva(r) { irAReserva(r.id) } } }
```

### 8.5 El QR

`reservation.qr` es un texto firmado por el servidor (`ML1.<id>.<versión>.<firma>`).
La app **solo lo dibuja** con ZXing (`CodigoQr` en `ui/reservas/Detalle.kt`):
nunca lo genera ni lo modifica. Si la reserva cambia, el servidor da uno nuevo
y el anterior deja de valer.

---

## 9. Formularios

Ejemplo: crear cuenta (así está en `ui/acceso/Acceso.kt`).

```kotlin
class RegistroVM : ViewModel() {
    var nombre by mutableStateOf("")
    var email by mutableStateOf("")
    var telefono by mutableStateOf("")
    var clave by mutableStateOf("")
    var clave2 by mutableStateOf("")
    var cargando by mutableStateOf(false)
        private set
    var errores by mutableStateOf<Map<String, String>>(emptyMap())   // campo → mensaje
        private set

    fun crear() {
        if (cargando) return                               // evita el doble envío
        if (clave != clave2) {                             // comprobación solo de la app
            errores = mapOf("password2" to "Las contraseñas no coinciden.")
            return
        }
        viewModelScope.launch {
            cargando = true
            errores = emptyMap()
            try {
                val r = App.api.post("/auth/register", JSONObject()
                    .put("name", nombre.trim()).put("email", email.trim())
                    .put("phone", telefono.trim()).put("password", clave))
                App.sesion.entrar(r)                       // ya tiene sesión: la app cambia sola
            } catch (e: ApiError) {
                errores = mapOf((e.campo ?: "_") to e.texto)   // "_" = error sin campo
            } finally {
                cargando = false
            }
        }
    }
}

@Composable
fun RegistroPantalla(irAEntrar: () -> Unit, vm: RegistroVM = viewModel()) {
    val e = vm.errores
    Pagina {
        Titulo("Crear cuenta")
        e["_"]?.let { Aviso(it) }                                   // error general, arriba
        Campo(vm.nombre, { vm.nombre = it }, "Nombre", error = e["name"], maximo = 80)
        Campo(vm.email, { vm.email = it }, "Correo electrónico", error = e["email"], teclado = KeyboardType.Email)
        Campo(vm.telefono, { vm.telefono = it }, "Teléfono", error = e["phone"], teclado = KeyboardType.Phone, maximo = 25)
        Campo(vm.clave, { vm.clave = it }, "Contraseña", error = e["password"], clave = true, maximo = 128,
              ayuda = "Al menos 10 caracteres. Evita contraseñas comunes.")
        Campo(vm.clave2, { vm.clave2 = it }, "Repite la contraseña", error = e["password2"], clave = true,
              ultimo = true, alTerminar = vm::crear)
        BotonPrincipal("Crear cuenta", vm.cargando,
            activo = vm.nombre.isNotBlank() && vm.email.isNotBlank() && vm.telefono.isNotBlank() && vm.clave.isNotEmpty(),
            alPulsar = vm::crear)
    }
}
```

1. Cada campo guarda su texto en el VM.
2. El botón se desactiva mientras falta algo, y muestra una rueda mientras se
   envía (`BotonPrincipal(…, cargando, activo = …)`).
3. Si la API responde con `field`, el mensaje sale **debajo de ese campo**.
4. Se valida un poco en la app (campo vacío), pero las reglas de verdad las
   comprueba la API (formato del teléfono, contraseña segura, correo repetido…).
5. Las contraseñas se ocultan (`clave = true`) y nunca se guardan en el móvil.

---

## 10. Todas las pantallas

| Pantalla | Ruta de navegación | Archivo | Rutas de la API que usa |
|---|---|---|---|
| Bienvenida | `bienvenida` | `ui/acceso/Acceso.kt` | — |
| Entrar | `entrar` | `ui/acceso/Acceso.kt` | `POST /auth/login` |
| Crear cuenta | `registro` | `ui/acceso/Acceso.kt` | `POST /auth/register` |
| He olvidado la contraseña | `recuperar` | `ui/acceso/Acceso.kt` | `POST /auth/forgot` |
| Inicio | `inicio` | `ui/inicio/Inicio.kt` | `GET /me`, `GET /me/notifications`, `GET /reservations`, `POST /me/notifications/read` |
| Reservar | `reservar` | `ui/reservas/Reservar.kt` | `GET /config`, `GET /availability` |
| Confirmar reserva | `confirmar/{fecha}/{personas}/{hora}` | `ui/reservas/Confirmar.kt` | `GET /config`, `POST /reservations` |
| Mis reservas | `reservas` | `ui/reservas/MisReservas.kt` | `GET /reservations`, `GET /group-requests`, `POST /group-requests/{id}/cancel` |
| Una reserva | `reserva/{id}` | `ui/reservas/Detalle.kt` | `GET /reservations/{id}`, `POST …/cancel`, `POST …/pay` |
| Modificar | `modificar/{id}` | `ui/reservas/Modificar.kt` | `GET /reservations/{id}`, `GET /availability`, `PATCH /reservations/{id}` |
| Grupo grande | `grupos` | `ui/reservas/Grupos.kt` | `GET /config`, `GET /group-requests/times`, `POST /group-requests` |
| Reserva del local | `vincular` | `ui/reservas/Vincular.kt` | `POST /reservations/link` |
| Mi cuenta | `cuenta` | `ui/cuenta/Cuenta.kt` | `PATCH /me`, `DELETE /me`, `POST /auth/logout`, `DELETE /me/devices` |

Para cambiar cómo se ve cada una, mira [pantallas.md](pantallas.md).

---

## 11. Navegación, enlaces y PayPal

### 11.1 Rutas de navegación (`ui/Navegacion.kt`)

```kotlin
@Composable
fun RaizApp() {
    val usuario by App.sesion.usuario.collectAsState()
    if (usuario == null) AccesoNav() else PrincipalNav()   // sin sesión: acceso; con sesión: cliente
}

NavHost(nav, startDestination = "inicio") {
    composable("inicio") { InicioPantalla(irAReserva = { nav.navigate("reserva/$it") }, …) }
    composable("reserva/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) {
        DetallePantalla(irAModificar = { nav.navigate("modificar/$it") })
    }
    …
}
```

- Cada pantalla tiene un **nombre de ruta** (`"inicio"`, `"reserva/{id}"`).
- `nav.navigate("reserva/15")` abre una. `nav.popBackStack()` vuelve atrás.
- Los VM leen los parámetros de la ruta con `SavedStateHandle` (`estado.get<Int>("id")`).
- Cuando la sesión caduca, `usuario` pasa a `null` y la app vuelve sola a «Entrar».

### 11.2 Pago de la señal con PayPal

1. «Pagar con PayPal» → `POST /reservations/{id}/pay` con `{"from": "app"}`.
2. La API crea el pedido en PayPal y devuelve `approve_url`.
3. La app lo abre en una **pestaña del navegador** (Custom Tabs), no en un
   WebView. Así el usuario ve la dirección de PayPal y su candado.
4. El usuario paga. PayPal vuelve a la API (`/payments/return`), que cobra y redirige a
   `mesalista://reserva/{id}?pago=ok`.
5. El `intent-filter` del manifiesto hace que Android abra la app con ese enlace.
   `Enlaces.recibir()` lo valida (solo números y resultados conocidos) y la app
   abre la reserva, muestra el resultado y recarga.

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="mesalista" android:host="reserva" />
</intent-filter>
```

---

## 12. Configurar Firebase (avisos push)

Firebase se usa **solo para los avisos push**. Las reservas y las cuentas están
en la API, no en Firestore: Firestore no puede impedir que dos personas
reserven la misma mesa a la vez.

### 12.1 En la consola de Firebase

1. console.firebase.google.com > *Agregar proyecto* (MesaLista usa `mesalista-57287`).
2. *Agregar app* > **Android**. *Nombre del paquete*: **`com.mesalista.cliente`**,
   igual que `applicationId` en `app/build.gradle.kts`.
3. Descarga **`google-services.json`** y guárdalo en **`app/google-services.json`**.
   No se sube a Git (está en `.gitignore`).
4. *Configuración del proyecto > Cuentas de servicio > Generar nueva clave privada*:
   ese archivo es para **el servidor**, nunca para la app. En la máquina de
   Azure se instala con `sudo mesalista push <archivo.json>`.

### 12.2 En el proyecto

Raíz (`build.gradle.kts`): plugin `com.google.gms.google-services`.

`app/build.gradle.kts`: se aplica solo si existe el archivo, así el proyecto
compila también sin Firebase.

```kotlin
val conFirebase = file("google-services.json").exists()
if (conFirebase) apply(plugin = "com.google.gms.google-services")
android { defaultConfig { buildConfigField("boolean", "CON_FIREBASE", conFirebase.toString()) } }
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")   // .await()
}
```

### 12.3 En el código

**1. Canal de avisos** (Android 8+), al arrancar (`Avisos.crearCanal`):

```kotlin
val canal = NotificationChannel("reservas", "Reservas", NotificationManager.IMPORTANCE_HIGH)
contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
```

**2. Permiso** (Android 13+), una vez, al entrar (`Navegacion.kt`):

```kotlin
val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
```

**3. Registrar el móvil en la API**, con sesión, al abrir la app (`Avisos.registrar`):

```kotlin
val token = FirebaseMessaging.getInstance().token.await()
App.api.post("/me/devices", JSONObject().put("token", token).put("platform", "android"))
```

**4. Servicio de Firebase** (`AvisosServicio.kt`, declarado en el manifiesto):

```kotlin
class AvisosServicio : FirebaseMessagingService() {
    override fun onNewToken(token: String) {               // Firebase cambia el token
        alcance.launch { Avisos.registrar(token) }
    }
    override fun onMessageReceived(m: RemoteMessage) {     // aviso con la app abierta
        Avisos.mostrar(this, m.notification?.title ?: "", m.notification?.body ?: "", m.data)
        Avisos.cambios.tryEmit(Unit)                       // las pantallas abiertas recargan
    }
}
```

**5. Al cerrar sesión**: `DELETE /me/devices` y `FirebaseMessaging.getInstance().deleteToken()`.
Sin sesión, el móvil no recibe avisos de nadie.

### 12.4 Qué trae cada aviso

El servidor envía `notification.title` y `notification.body` (fecha, hora y
comensales) y en `data`:

| `data.type` | Cuándo |
|---|---|
| `confirmed` | Reserva confirmada (también al pagar la señal) |
| `modified` | Reserva modificada: el QR anterior ya no vale |
| `cancelled_holder` / `cancelled_restaurant` | Cancelada por el cliente / por el restaurante |
| `group_accepted` / `group_rejected` | Grupo aceptado (falta pagar) / no aceptado |
| `payment_expired` | No se pagó la señal a tiempo |

`data.reservation_id`, si viene, abre esa reserva al tocar el aviso. Los avisos
nunca llevan el QR, el menú ni la nota. El correo electrónico solo se usa para
restablecer la contraseña. Todo lo demás llega por push.

### 12.5 Probar

1. Instala la app con `google-services.json` y entra con una cuenta.
2. En el servidor, `sudo mesalista estado` debe mostrar «Avisos push (Firebase):
   cuenta de servicio aceptada · N móviles registrados».
3. Haz una reserva o cancélala: llega el aviso. Con la app cerrada lo muestra el
   sistema; con la app abierta lo muestra la app.

---

## 13. Seguridad

- **Token cifrado**: `Sesion.kt` lo guarda en `SharedPreferences` cifrado con
  AES-GCM y una clave del **Android Keystore**, que no sale del móvil.
- **Sin copias en la nube**: `android:allowBackup="false"` y reglas de copia vacías.
- **Solo HTTPS** en release, y solo certificados del sistema (no los que instale el usuario).
- **Ningún secreto en la app**: ni claves de PayPal ni la cuenta de servicio de
  Firebase. `google-services.json` no es secreto, pero no se sube a Git.
- **Los enlaces se validan**: `Enlaces.kt` solo acepta un número de reserva y
  resultados conocidos, porque cualquiera puede abrir un enlace `mesalista://`.
- **PayPal en el navegador**, no en un WebView propio.
- **Los registros** (Logcat) nunca deben mostrar el token ni contraseñas.

---

## 14. Probar y encontrar errores

| Herramienta | Para qué |
|---|---|
| **Logcat** (Android Studio, abajo) | Errores y trazas de la app. Filtra por `com.mesalista.cliente` |
| **Vista previa** (`@Preview`) | Ver una pantalla sin ejecutar la app |
| **App Inspection > Network Inspector** | Ver cada petición a la API y su respuesta |
| `curl` / Postman | Probar la API sin la app (apartado 7.7) |
| `sudo mesalista estado` / `logs` | Estado y mensajes del servidor |

Problemas frecuentes:

| Síntoma | Causa | Solución |
|---|---|---|
| «No se puede conectar con el servidor» | Dirección de la API mal, sin red o API parada | `gradle.properties` + *Sync Now*; `sudo mesalista estado` |
| `CLEARTEXT communication not permitted` | HTTP en release o hacia otra IP | Usa HTTPS o la excepción de `src/debug` |
| Vuelve sola a «Entrar» | Token caducado (60 min) | Normal: volver a entrar |
| No llegan avisos | Falta `google-services.json`, permiso denegado o servidor sin cuenta de servicio | Apartado 12.5 |
| PayPal no vuelve a la app | Falta el `intent-filter` o `front.payment_app` mal en el servidor | Apartado 11.2 |
| `Unresolved reference` al compilar | Falta un `import` o una librería | Alt+Enter sobre el nombre en rojo |

---

## 15. Compilar y publicar

1. Sube `versionCode` (1, 2, 3…) y `versionName` («1.1»…) en `app/build.gradle.kts`.
2. Comprueba que `mesalista.api` es la dirección HTTPS de producción.
3. Firma con la **misma** clave de siempre (`keystore.properties` y `keystore/`;
   el README explica cómo crearla la primera vez).
4. `./gradlew assembleRelease`, o en Android Studio *Build > Generate Signed App Bundle / APK*.
5. El APK queda en `app/build/outputs/apk/release/app-release.apk`. Cópialo a
   `release/MesaLista-<versión>.apk` y actualiza el README.

Si pierdes la clave de firma, las versiones nuevas no se podrán instalar
encima de las anteriores: guárdala con sus contraseñas en un lugar seguro.

---

## 16. Ejemplo completo: añadir una pantalla que llama a la API

Objetivo: una pantalla «El restaurante» que muestra los comensales máximos y
las opciones de menú, con `GET /config`.

**1. El modelo ya existe** (`Config` en `datos/Modelos.kt`, con `Datos.config()`).

**2. La pantalla** (`ui/inicio/Restaurante.kt`):

```kotlin
package com.mesalista.cliente.ui.inicio

class RestauranteVM : ViewModel() {
    var cfg by mutableStateOf<Config?>(null); private set
    var error by mutableStateOf<String?>(null); private set

    fun cargar() {
        viewModelScope.launch {
            try { cfg = Datos.config() } catch (e: ApiError) { error = e.texto }
        }
    }
}

@Composable
fun RestaurantePantalla(vm: RestauranteVM = viewModel()) {
    LaunchedEffect(Unit) { vm.cargar() }
    Pagina {
        Titulo("El restaurante")
        val c = vm.cfg
        when {
            vm.error != null -> { Aviso(vm.error!!); BotonSecundario("Reintentar") { vm.cargar() } }
            c == null -> CircularProgressIndicator()
            else -> {
                Text("Reservas de ${c.minPersonas} a ${c.maxPersonas} personas, hasta ${c.diasAntelacion} días antes.")
                Text("Grupos de hasta ${c.maxGrupo} personas, con solicitud.")
                Seccion("Opciones de menú")
                c.opcionesMenu.forEach { Text("• ${it.nombre}") }
            }
        }
    }
}
```

**3. La ruta** en `ui/Navegacion.kt`, dentro del `NavHost` de `PrincipalNav`:
`composable("restaurante") { RestaurantePantalla() }`, y su título en `titulo()`.

**4. El acceso**: un botón en `InicioPantalla` con un parámetro nuevo
`irARestaurante: () -> Unit`, y en `Navegacion.kt`
`irARestaurante = { nav.navigate("restaurante") }`.

**5. Probar**: ▶ Run. Con la API caída debe salir el aviso y «Reintentar».

---

## 17. Lista de comprobación antes de entregar

- [ ] Todas las pantallas tienen cargando, error con «Reintentar», vacío y datos.
- [ ] Ningún texto de error inventado: se muestra el `message` de la API.
- [ ] Los errores con `field` salen debajo de su campo.
- [ ] Los botones de enviar se desactivan mientras se envía.
- [ ] `POST /reservations` lleva un `request_id` por intento.
- [ ] Al recibir `401`, la app vuelve a «Entrar».
- [ ] Release solo con HTTPS; `allowBackup="false"`; token cifrado.
- [ ] Avisos: permiso, canal, `POST /me/devices` al entrar y `DELETE` al salir.
- [ ] Se ve bien en modo claro, oscuro y con letra grande.
- [ ] `versionCode` subido y APK firmado con la misma clave.

---

## 18. Glosario

| Término | Qué es |
|---|---|
| **API** | Programa del servidor al que la app pide datos y acciones por internet |
| **REST** | Forma de organizar una API: rutas (recursos) + métodos HTTP (GET, POST…) |
| **Ruta / endpoint** | Dirección de una operación de la API, como `/reservations` |
| **JSON** | Formato de texto de los datos: `{"clave": "valor"}` |
| **HTTP / HTTPS** | Protocolo de la web; la S es cifrado |
| **Código HTTP** | Número que dice cómo ha ido: 200 bien, 404 no existe, 409 conflicto… |
| **Token Bearer** | Llave temporal que da la API al entrar; va en `Authorization` |
| **nginx** | Servidor web que recibe el HTTPS y reparte entre la web y la API |
| **Kotlin** | Lenguaje de programación de la app |
| **Jetpack Compose** | Forma de dibujar pantallas con funciones de Kotlin |
| **Composable** | Función marcada con `@Composable` que dibuja una parte de la pantalla |
| **Material 3** | Estilo de componentes de Google (botones, tarjetas, barras) |
| **ViewModel** | Clase que guarda el estado de una pantalla |
| **Estado** | Datos que, al cambiar, hacen que la pantalla se redibuje |
| **Corrutina** | Tarea que espera (a la red) sin congelar la pantalla |
| **Gradle** | Herramienta que compila el proyecto y baja las librerías |
| **BuildConfig** | Constantes generadas al compilar (como la dirección de la API) |
| **debug / release** | Versión de desarrollo / versión para instalar en los móviles |
| **APK** | Archivo instalable de una app Android |
| **Firebase / FCM** | Servicios de Google; FCM es el que entrega los avisos push |
| **Deep link** | Enlace (`mesalista://…`) que abre la app en una pantalla concreta |
| **Custom Tabs** | Pestaña del navegador abierta desde la app (para PayPal) |
| **Keystore** | Almacén seguro de claves del móvil, o el archivo con la clave de firma |
| **UUID** | Identificador aleatorio único, como `3f1c…` |
| **Idempotencia** | Repetir una petición no cambia el resultado (el `request_id`) |
