# Cambiar el aspecto de la app: qué archivo toca cada pantalla

La app separa, como la web, **cómo se ve** de **lo que hace**. Cada pantalla es
un archivo de Kotlin con dos partes:

| Parte | Dónde, dentro del archivo | Qué cambiar ahí |
|---|---|---|
| Lo que hace | `class …VM : ViewModel()` (arriba) | Qué pide a la API, qué datos guarda y qué pasa al pulsar un botón |
| **Cómo se ve** | **`@Composable fun …Pantalla(…)`** (abajo) | **Textos, orden de los bloques, botones, tarjetas** (Jetpack Compose, Material 3) |

Todas las carpetas de abajo están dentro de
`app/src/main/java/com/mesalista/cliente/`.

## Ejemplo: la pantalla de inicio

Archivo: **`ui/inicio/Inicio.kt`**

- `class InicioVM`: pide `/me`, los avisos del restaurante y las reservas.
- `fun InicioPantalla`: el saludo «Hola, …», la tarjeta de avisos, el botón
  «Reservar mesa», las próximas reservas y «Añadir una reserva hecha en el local».

Para cambiar un texto o el orden, se edita `InicioPantalla`. Por ejemplo,
`BotonPrincipal("Reservar mesa", …)` es el botón grande.

## Todas las pantallas

### Sin sesión (acceso)

| Pantalla | Ruta en `Navegacion.kt` | Archivo | Función que la dibuja |
|---|---|---|---|
| Bienvenida (la primera al abrir la app) | `bienvenida` | `ui/acceso/Acceso.kt` | `BienvenidaPantalla` |
| Entrar | `entrar` | `ui/acceso/Acceso.kt` | `EntrarPantalla` (lógica en `EntrarVM`) |
| Crear cuenta | `registro` | `ui/acceso/Acceso.kt` | `RegistroPantalla` (`RegistroVM`) |
| He olvidado la contraseña | `recuperar` | `ui/acceso/Acceso.kt` | `RecuperarPantalla` (`RecuperarVM`) |

### Con sesión (barra inferior: Inicio · Reservar · Mis reservas · Cuenta)

| Pantalla | Ruta | Archivo | Función que la dibuja |
|---|---|---|---|
| Inicio | `inicio` | `ui/inicio/Inicio.kt` | `InicioPantalla` (`InicioVM`) |
| Reservar: día, personas y hora | `reservar` | `ui/reservas/Reservar.kt` | `ReservarPantalla` (`ReservarVM`) |
| Confirmar reserva (menú, nota) | `confirmar/{fecha}/{personas}/{hora}` | `ui/reservas/Confirmar.kt` | `ConfirmarPantalla` (`ConfirmarVM`) |
| Mis reservas | `reservas` | `ui/reservas/MisReservas.kt` | `MisReservasPantalla` (`MisReservasVM`) |
| Una reserva (QR, cancelar, pagar señal) | `reserva/{id}` | `ui/reservas/Detalle.kt` | `DetallePantalla` (`DetalleVM`); el QR en `CodigoQr` |
| Modificar reserva | `modificar/{id}` | `ui/reservas/Modificar.kt` | `ModificarPantalla` (`ModificarVM`) |
| Solicitud de grupo grande | `grupos` | `ui/reservas/Grupos.kt` | `GrupoPantalla` (`GrupoVM`); cada solicitud en `TarjetaSolicitud` |
| Añadir una reserva hecha en el local | `vincular` | `ui/reservas/Vincular.kt` | `VincularPantalla` (`VincularVM`) |
| Mi cuenta (datos, avisos del móvil, cerrar sesión, eliminar cuenta) | `cuenta` | `ui/cuenta/Cuenta.kt` | `CuentaPantalla` (`CuentaVM`) |

## Piezas comunes

Si cambias una de estas piezas, cambia en todas las pantallas que la usan.

| Qué | Archivo |
|---|---|
| Rutas, barra superior (títulos), barra inferior (secciones e iconos) | `ui/Navegacion.kt` (`titulo()`, `enum class Seccion`) |
| Márgenes y ancho de las pantallas (`Pagina`), campos de texto (`Campo`), botones (`BotonPrincipal`, `BotonSecundario`), avisos de error (`Aviso`), títulos (`Titulo`) | `ui/comun/Componentes.kt` |
| Tarjeta de una reserva en las listas (`TarjetaReserva`), etiqueta de estado (`EstadoChip`) y subtítulos (`Seccion`) | `ui/comun/Reservas.kt` |
| Selectores de día, personas y hora, y el formulario de menú y nota | `ui/reservas/Selectores.kt` |
| **Colores de toda la app** (claro y oscuro) | **`ui/tema/Tema.kt`** (`Marca` es el color principal, #A8432A como en la web) |
| Colores de la ventana y de la pantalla de carga | `app/src/main/res/values/colors.xml` y `values-night/colors.xml`; estilos en `res/values/themes.xml` |
| Nombre de la app | `app/src/main/res/values/strings.xml` (`app_name`) |
| Icono | `app/src/main/res/mipmap-anydpi-v26/` y `res/drawable/` (en Android Studio: botón derecho en `res` > New > Image Asset) |
| Fechas y textos de estado («Confirmada», «sábado 21 de noviembre»…) | `datos/Formato.kt` |

Lo de `datos/` (`Api.kt`, `Sesion.kt`, `Avisos.kt`, `Enlaces.kt`) es la
comunicación con el servidor y Firebase: no hace falta tocarlo para cambiar el aspecto.

## Cambiar una pantalla

1. Abre su archivo (tabla de arriba) y busca la función `…Pantalla`.
2. Cambia los textos, el orden o los componentes. Usa los de `ui/comun/` para
   que se vea como el resto de la app.
3. No cambies los nombres de los parámetros (`irAReserva`, `alConfirmar`…):
   `Navegacion.kt` los usa para ir de una pantalla a otra.
4. Para verlo sin ejecutar la app, añade una vista previa debajo de la función:

   ```kotlin
   @Preview(showBackground = true)
   @Composable
   private fun BienvenidaVista() = TemaMesaLista { BienvenidaPantalla(irAEntrar = {}, irARegistro = {}) }
   ```

   Necesita `import androidx.compose.ui.tooling.preview.Preview` e
   `import com.mesalista.cliente.ui.tema.TemaMesaLista`. Android Studio la
   muestra en el panel **Split** o **Design**.
   Las pantallas que llaman a la API (con `…VM`) se ven mejor ejecutando la app
   en el emulador.

## Añadir una pantalla nueva

Ejemplo: «Sobre el restaurante», en la sección Inicio.

1. Crea `ui/inicio/Restaurante.kt`:

   ```kotlin
   package com.mesalista.cliente.ui.inicio

   import androidx.compose.material3.Text
   import androidx.compose.runtime.Composable
   import com.mesalista.cliente.ui.comun.Pagina
   import com.mesalista.cliente.ui.comun.Titulo

   @Composable
   fun RestaurantePantalla() {
       Pagina {
           Titulo("Sobre el restaurante")
           Text("Horario, dirección…")
       }
   }
   ```

2. En `ui/Navegacion.kt`, dentro del `NavHost` de `PrincipalNav`, añade
   `composable("restaurante") { RestaurantePantalla() }` y su título en `titulo()`:
   `ruta == "restaurante" -> "El restaurante"`.
3. Añade un botón que la abra desde otra pantalla. Por ejemplo, en `InicioPantalla`
   añade un parámetro `irARestaurante: () -> Unit` y, en `Navegacion.kt`,
   pásale `irARestaurante = { nav.navigate("restaurante") }`.
4. Si debe ser una sección de la barra inferior, añádela a `enum class Seccion`.
   Ponla como mucho en 5 secciones, para que se lea en móviles pequeños.

## Ver los cambios

- En Android Studio, pulsa ▶ (Run) con el emulador o un móvil conectado.
  Los cambios pequeños en las pantallas se aplican con **Apply Changes**, sin
  reiniciar la app.
- La versión `debug` llama a la API de desarrollo de tu ordenador (README,
  «Desarrollo con el emulador»). Para probar contra el servidor de Azure,
  instala la versión `release` (README, «Versión para los móviles»).
