# MesaLista · app para Android (Firebase)

App de MesaLista para **clientes**, nativa de Android, hecha con **Kotlin y
Jetpack Compose** (Material 3). No guarda reservas: todo se lo pide al servidor
de MesaLista ([LXPT-UIB/Servidor](https://github.com/LXPT-UIB/Servidor), Node.js detrás de nginx)
por su API `/api/v1`, la misma que usa la web ([LXPT-UIB/WEB](https://github.com/LXPT-UIB/WEB)).
**Firebase** se usa solo para los **avisos push** (FCM).

Las tres partes de MesaLista, cada una en su repositorio:

| Parte | Repositorio |
|---|---|
| Servidor: API REST (Node.js + PostgreSQL) y nginx | [LXPT-UIB/Servidor](https://github.com/LXPT-UIB/Servidor) |
| Web de clientes y panel del restaurante (Node.js) | [LXPT-UIB/WEB](https://github.com/LXPT-UIB/WEB) |
| App para Android de los clientes (Kotlin, Firebase) | [LXPT-UIB/APP](https://github.com/LXPT-UIB/APP) |

```
App (Kotlin) ──HTTPS, token Bearer──> Servidor /api/v1 ──aviso──> Firebase (FCM) ──> móvil
```

- El token de la sesión se guarda **cifrado** (AES-GCM con una clave del Android Keystore).
- No hay copias de seguridad en la nube de los datos de la app.
- La versión que se instala en los móviles (`release`) solo habla por **HTTPS** con certificados del sistema.

## Descargar e instalar (versión 1.1)

[`release/MesaLista-1.1.apk`](release/MesaLista-1.1.apk) es la versión de
producción, firmada con la clave de MesaLista. Llama a
`https://mesalista.spaincentral.cloudapp.azure.com/api/v1` y trae los avisos push
de Firebase (proyecto `mesalista-57287`).

1. Descarga el APK en el móvil (Android 8.0 o posterior).
2. Ábrelo y permite «Instalar apps desconocidas» para el navegador o el gestor de archivos.
3. Las versiones siguientes se instalan encima si van firmadas con la misma clave
   (`keystore.properties`, no se sube a Git).

| Dato | Valor |
|---|---|
| Paquete | `com.mesalista.cliente` · versión 1.1 (versionCode 2) |
| SHA-256 del APK | `86891376bc1904de2f45069685a6ada62b2d64681eebcbb829f8dfdc3abff4df` |
| SHA-256 del certificado de firma | `4c533e022a7c4965e1ec45f46012c6e0c0b8110bc0eedeb249a4f27ba856c442` |

Cambios de la 1.1: «Mis reservas» separa las solicitudes de grupo que esperan
al local o el pago, las no aceptadas y las anteriores; los textos ya no dicen
que se envía un correo (los avisos llegan por la app). La 1.0 sigue en
[`release/MesaLista-1.0.apk`](release/MesaLista-1.0.apk).

## Carpetas

| Carpeta o archivo | Qué hay |
|---|---|
| `gradle.properties` | `mesalista.api`: dirección de la API (release) y `mesalista.api.debug` (desarrollo) |
| `app/build.gradle.kts` | Versión, librerías, firma y si Firebase está activo |
| `app/src/main/java/com/mesalista/cliente/datos/` | `Api` (llamadas al servidor), `Sesion` (token cifrado), `Avisos` (push), `Enlaces` y `Acciones` |
| `app/src/main/java/com/mesalista/cliente/AvisosServicio.kt` | Servicio de Firebase: token nuevo y avisos con la app abierta |
| `app/src/main/java/com/mesalista/cliente/ui/` | Pantallas de Compose: `acceso/`, `inicio/`… y `Navegacion.kt` |
| `app/src/main/java/com/mesalista/cliente/ui/tema/Tema.kt` | Colores de la app (aquí se cambia el diseño) |
| `app/src/main/res/` | Icono, textos y seguridad de red (`xml/network_security_config.xml`) |
| `docs/pantallas.md` | Qué archivo cambiar para cada pantalla (aspecto, textos, pantallas nuevas) |
| `docs/manual-estilo.md` | Manual: cambiar colores, letra, formas, distribución, textos e icono |
| `app/src/debug/` | Solo en desarrollo: permite HTTP hacia `10.0.2.2` (la API de este ordenador) |

## Compilar

Con Android Studio: abrir esta carpeta y pulsar ▶ (Run). Por línea de órdenes (JDK 17):

```bash
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

### Versión para los móviles (release)

Llama a `mesalista.api` de `gradle.properties` (HTTPS) y va reducida con R8.
Para instalarla tiene que ir **firmada**. La clave de firma se crea una vez y no
se sube a Git; sin ella sale `app-release-unsigned.apk`, que no se puede instalar.

```bash
mkdir keystore
keytool -genkeypair -v -keystore keystore/mesalista.jks -alias mesalista -keyalg RSA -keysize 2048 -validity 10000
```

`keytool` pide las contraseñas. Después se crea `keystore.properties` en la raíz
del proyecto con:

```
storeFile=keystore/mesalista.jks
storePassword=<contraseña del almacén>
keyAlias=mesalista
keyPassword=<contraseña de la clave>
```

Y se compila:

```bash
./gradlew assembleRelease
```

El APK firmado queda en `app/build/outputs/apk/release/app-release.apk`. Guarda
la clave y sus contraseñas: las versiones nuevas tienen que firmarse con la misma
para instalarse encima de la anterior.

### Desarrollo con el emulador

La versión `debug` llama a `http://10.0.2.2:8091/api/v1`: desde el emulador de
Android, `10.0.2.2` es el propio ordenador. Basta con tener arrancada la API
del servidor ([LXPT-UIB/Servidor](https://github.com/LXPT-UIB/Servidor),
Node.js 20 o posterior y PostgreSQL) en este ordenador:

```bash
npm install
cp config/config.example.json config/config.json
node src/server.js        # escucha en http://127.0.0.1:8091
```

En ese `config.json` de desarrollo hay que poner los datos de PostgreSQL, un
`qr_secret` y `"require_https": false`: desde el emulador la API llega por HTTP,
y con `true` la rechazaría. En el servidor de Azure se queda en `true` (nginx
pone el HTTPS).

## Firebase (avisos push)

1. En [console.firebase.google.com](https://console.firebase.google.com) crear
   un proyecto y añadir una app **Android** con el paquete **`com.mesalista.cliente`** (el `applicationId` de `app/build.gradle.kts`).
2. Descargar `google-services.json` y guardarlo en `app/google-services.json`
   (no se sube a Git). Sin ese archivo la app compila y funciona, pero sin avisos.
3. La **clave de la cuenta de servicio** (Configuración del proyecto → Cuentas de
   servicio → Generar nueva clave privada) es **secreta**: no va en la app. Se
   instala en el servidor con `sudo mesalista push <archivo.json>`, y
   `sudo mesalista estado` comprueba que Google la acepta.

Cómo funcionan en la app (`datos/Avisos.kt` y `AvisosServicio.kt`):

- Al entrar, Android 13 o posterior pregunta una vez si se permiten los avisos.
  Si se deniegan, en **Cuenta → Avisos en este móvil** hay un botón que abre los
  ajustes del móvil.
- Con sesión, la app registra el móvil en el servidor (`POST /me/devices`) al
  abrirse y cada vez que Firebase le da un token nuevo.
- Al cerrar sesión lo da de baja (`DELETE /me/devices`). Salga como salga
  (también si la sesión caduca o se elimina la cuenta), anula el token: sin
  sesión, el móvil no recibe avisos de nadie.
- Los avisos van al canal **Reservas**. Con la app cerrada los muestra el
  sistema; con la app abierta los muestra la app y recarga la pantalla. Al
  tocarlos se abre la reserva (`data.reservation_id`) o, si no hay, **Mis reservas**.
- Solo traen la fecha, la hora y los comensales: nunca el QR, las opciones ni la nota.

Para probarlos de verdad, el servidor (también el de desarrollo) necesita la
cuenta de servicio del mismo proyecto de Firebase. Sin ella, el servidor guarda
los avisos en `push_outbox` pero no los envía.
