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
| `app/src/main/java/com/mesalista/cliente/datos/` | `Api` (llamadas al servidor), `Sesion` (token cifrado), `Acciones` |
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
