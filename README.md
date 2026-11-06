# MesaLista · app para Android (Firebase)

App de MesaLista para **clientes**, nativa de Android, hecha con **Kotlin y
Jetpack Compose** (Material 3). No guarda reservas: todo se lo pide al servidor
de MesaLista ([MesaLista-Servidor](https://github.com/LXPT-UIB/MesaLista-Servidor))
por su API `/api/v1`. **Firebase** se usa solo para los **avisos push** (FCM).

```
App (Kotlin) ──HTTPS, token Bearer──> Servidor /api/v1 ──aviso──> Firebase (FCM) ──> móvil
```

- El token de la sesión se guarda **cifrado** (AES-GCM con una clave del Android Keystore).
- No hay copias de seguridad en la nube de los datos de la app.
- La versión que se instala en los móviles (`release`) solo habla por **HTTPS** con certificados del sistema.

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
Android, `10.0.2.2` es el propio ordenador. Basta con tener arrancada la API de
desarrollo del servidor:

```bash
php -d extension=pdo_pgsql -S 127.0.0.1:8091 -t publico pruebas/dev-router.php
```

## Firebase (avisos push)

1. En [console.firebase.google.com](https://console.firebase.google.com) crear
   un proyecto y añadir una app **Android** con el paquete **`com.mesalista.cliente`** (el `applicationId` de `app/build.gradle.kts`).
2. Descargar `google-services.json` y guardarlo en `app/google-services.json`
   (no se sube a Git). Sin ese archivo la app compila y funciona, pero sin avisos.
3. La **clave de la cuenta de servicio** (Configuración del proyecto → Cuentas de
   servicio → Generar nueva clave privada) es **secreta**: no va en la app. Se
   instala en el servidor con `configure_fcm.php`.

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
