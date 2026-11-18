# Manual: cambiar el estilo, la apariencia, la distribución y los textos de la app

Este manual explica **cómo** cambiar el aspecto de la app. Para saber **qué
archivo** corresponde a cada pantalla, mira [pantallas.md](pantallas.md).

La app está hecha con **Jetpack Compose** y **Material 3**: las pantallas no
usan XML, se escriben en Kotlin con piezas como `Text`, `Button`, `Card`,
`Column` y `Row`. Las rutas de abajo empiezan en
`app/src/main/java/com/mesalista/cliente/`.

| Quieres cambiar… | Archivo |
|---|---|
| Colores de toda la app (claro y oscuro) | `ui/tema/Tema.kt` |
| Letra, formas y botones de toda la app | `ui/tema/Tema.kt` y `ui/comun/Componentes.kt` |
| Distribución y textos de una pantalla | Su archivo en `ui/…` (función `…Pantalla`) |
| Barra superior, barra inferior y títulos | `ui/Navegacion.kt` |
| Nombre, icono y pantalla de carga | `app/src/main/res/` |

---

## 1. Antes de empezar

1. Abre la carpeta del proyecto en **Android Studio**.
2. Para ver una pantalla mientras la cambias, usa una **vista previa**
   (`@Preview`, ver [pantallas.md](pantallas.md), «Cambiar una pantalla») y el panel
   **Split**. Las pantallas que cargan datos de la API se ven mejor en el emulador.
3. Pulsa ▶ (Run) para abrir la app en el emulador o en un móvil. Tras un cambio
   pequeño, **Apply Changes** (el rayo junto a ▶) lo aplica sin reiniciar.
4. Mira cada cambio **en modo claro y oscuro** (en el emulador: Ajustes >
   Pantalla > Tema oscuro) y con la **letra grande** (Ajustes > Pantalla >
   Tamaño de fuente). La app sigue los dos ajustes del móvil.

---

## 2. Colores

Todos los colores de las pantallas salen de `ui/tema/Tema.kt`: Material 3
reparte los colores del tema entre botones, tarjetas, fondos y textos.

```kotlin
val Marca = Color(0xFFA8432A)        // color principal, el mismo que la web

private val Claro = lightColorScheme(
    primary = Marca,                  // botones principales, enlaces, elementos activos
    onPrimary = Color.White,          // texto encima de primary
    primaryContainer = Color(0xFFF6E7E2),   // fondos suaves (avisos del restaurante)
    background = Color(0xFFFBF9F7),   // fondo de las pantallas
    surfaceContainerHigh = Color(0xFFEFE9E3), // fondo de las tarjetas de reserva
    error = Color(0xFFB3261E),
    …
)
private val Oscuro = darkColorScheme( … )   // los mismos nombres, para el modo oscuro
```

Los colores se escriben como `Color(0xFF` + el código hexadecimal: `#2E7D5B` es
`Color(0xFF2E7D5B)`.

### Cambiar el color de la marca

1. En `Tema.kt`, cambia `Marca` y los tonos que lo acompañan en `Claro`:
   `primaryContainer` (versión muy clara) y `onPrimaryContainer` (versión muy oscura).
2. En `Oscuro`, pon una versión **más clara** del color en `primary` y una oscura
   en `primaryContainer`: sobre fondo oscuro, el color de la marca tal cual se lee mal.
3. En `app/src/main/res/values/colors.xml`, cambia `bar`: es el fondo del
   icono y de la pantalla de carga.
4. Para que la web se vea igual, cambia también `--ml-marca` en la web
   (`publico/assets/css/mesalista.css` del repositorio WEB).

Una herramienta cómoda para crear las dos paletas a partir de un color es
**Material Theme Builder** (búscalo en internet). Exporta en «Jetpack Compose»
y copia los valores en `Claro` y `Oscuro`.

### Colores que no vienen del tema

| Qué | Dónde |
|---|---|
| Etiquetas de estado de las reservas (verde Confirmada, amarillo Pendiente…) | `ui/comun/Reservas.kt`, función `coloresEstado` |
| Fondo de la ventana al abrir la app | `res/values/colors.xml` (`fondo`) y `res/values-night/colors.xml` |

Comprueba que el texto se lee bien sobre cada color: el contraste mínimo
recomendado es 4,5:1.

---

## 3. Letra (tipografía)

La app usa la letra del móvil (Roboto en la mayoría) con los estilos de
Material 3: `headlineSmall` para los títulos, `titleMedium` para los subtítulos,
`bodyLarge` y `bodyMedium` para el texto y `labelMedium` para las etiquetas.

### Cambiar tamaños o grosores en toda la app

En `ui/tema/Tema.kt`, crea una `Typography` y pásala al tema:

```kotlin
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Letras = Typography(
    headlineSmall = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),   // títulos de pantalla
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold), // subtítulos
)

@Composable
fun TemaMesaLista(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro,
        typography = Letras,
        content = contenido,
    )
}
```

Los estilos que no pongas siguen como estaban.

### Usar otra fuente

1. Descarga la fuente en `.ttf` (por ejemplo, de Google Fonts).
2. Cópiala en `app/src/main/res/font/` con un nombre en minúsculas y sin
   espacios: `inter_regular.ttf`, `inter_bold.ttf`. Crea la carpeta `font` si no existe.
3. En `Tema.kt`:

   ```kotlin
   import androidx.compose.ui.text.font.Font
   import androidx.compose.ui.text.font.FontFamily
   import com.mesalista.cliente.R

   private val Inter = FontFamily(
       Font(R.font.inter_regular, FontWeight.Normal),
       Font(R.font.inter_bold, FontWeight.Bold),
   )
   private val base = Typography()
   private val Letras = Typography(
       headlineSmall = base.headlineSmall.copy(fontFamily = Inter),
       titleMedium = base.titleMedium.copy(fontFamily = Inter),
       bodyLarge = base.bodyLarge.copy(fontFamily = Inter),
       bodyMedium = base.bodyMedium.copy(fontFamily = Inter),
       labelLarge = base.labelLarge.copy(fontFamily = Inter),
   )
   ```

   y pasa `typography = Letras` en `TemaMesaLista`, como arriba.

Usa siempre tamaños en `sp`, no en `dp`: así la letra respeta el tamaño que el
usuario ha elegido en su móvil.

---

## 4. Formas y botones

### Esquinas de las tarjetas y campos en toda la app

En `Tema.kt`, igual que la letra:

```kotlin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

private val Formas = Shapes(
    small = RoundedCornerShape(8.dp),     // campos de texto
    medium = RoundedCornerShape(16.dp),   // tarjetas
    large = RoundedCornerShape(24.dp),    // ventanas de diálogo
)
```

y añade `shapes = Formas` en `MaterialTheme(…)`.

### Botones

Los botones de todas las pantallas salen de `ui/comun/Componentes.kt`:

| Pieza | Qué es | Qué cambiar |
|---|---|---|
| `BotonPrincipal` | Botón relleno con el color de la marca | Alto (`heightIn(min = 52.dp)`), forma (`shape = …` en `Button`) |
| `BotonSecundario` | Botón con borde | Alto (`48.dp`), forma |
| `Campo` | Campo de texto con etiqueta y error | Estilo `OutlinedTextField` (con borde). Para relleno, cambia a `TextField` |
| `Aviso` | Tarjeta de error, aviso u ok | Colores por tipo, relleno (`padding(14.dp)`) |
| `Titulo` | Título y subtítulo de la pantalla | Estilos de letra |
| `Pagina` | Márgenes y ancho de todas las pantallas | Ver «Distribución» |

Ejemplo: botones principales con forma de píldora. En `BotonPrincipal`:

```kotlin
Button(onClick = alPulsar, enabled = activo && !cargando,
       shape = RoundedCornerShape(50),
       modifier = modifier.fillMaxWidth().heightIn(min = 52.dp)) { … }
```

---

## 5. Distribución (dónde va cada cosa)

### Cómo se colocan las cosas en Compose

| Pieza | Coloca a sus hijos… |
|---|---|
| `Column { … }` | uno **debajo** de otro |
| `Row { … }` | uno **al lado** de otro |
| `Box { … }` | uno **encima** de otro |
| `Pagina { … }` | como `Column`, con márgenes, desplazamiento y ancho máximo |

El **orden en el código es el orden en pantalla**. Para mover un bloque (por
ejemplo, poner «Próximas reservas» antes que el botón «Reservar mesa»), corta
sus líneas y pégalas en el sitio nuevo dentro de la misma `Pagina { … }`.

Ejemplo de `InicioPantalla` (`ui/inicio/Inicio.kt`), de arriba abajo:

```kotlin
Pagina {
    Titulo("Hola, …")                 // 1. saludo
    if (vm.avisos.isNotEmpty()) { … } // 2. avisos del restaurante
    BotonPrincipal("Reservar mesa", …)// 3. botón grande
    Seccion("Próximas reservas")      // 4. subtítulo
    …                                 // 5. lista de reservas
    BotonSecundario("Añadir una reserva hecha en el local", …) // 6.
}
```

### Márgenes y separaciones

- **Todas las pantallas:** en `Pagina` (`ui/comun/Componentes.kt`):
  `padding(horizontal = 20.dp, vertical = 16.dp)` son los márgenes,
  `spacedBy(12.dp)` la separación entre bloques y `widthIn(max = 520.dp)` el ancho
  máximo en tabletas.
- **Un elemento:** añádele `Modifier.padding(top = 16.dp)` (o `bottom`, `start`, `end`).
- **Un hueco:** `Spacer(Modifier.height(24.dp))` entre dos bloques.

### Barra superior y barra inferior

En `ui/Navegacion.kt`:

- **Títulos de la barra superior:** función `titulo()`.
- **Secciones de la barra inferior** (texto, icono y orden): `enum class Seccion`.

  ```kotlin
  Inicio("inicio", "Inicio", Icons.Filled.Home),
  Reservar("reservar", "Reservar", Icons.Filled.DateRange),
  ```

  El orden de las líneas es el orden en la barra. No cambies el primer texto
  (`"inicio"`…): es la ruta de la pantalla.
- **Iconos:** la app incluye los iconos básicos de Material (`Icons.Filled.Home`,
  `Person`, `DateRange`, `Settings`, `Star`…). Para otros (`Restaurant`,
  `EventSeat`…), añade en `app/build.gradle.kts`
  `implementation("androidx.compose.material:material-icons-extended")` junto a
  `material-icons-core`.

---

## 6. Textos

Los textos están escritos en el propio código, entre comillas.

| Texto | Dónde se cambia |
|---|---|
| Títulos, botones, etiquetas y párrafos de una pantalla | En su función `…Pantalla` (tabla en [pantallas.md](pantallas.md)): `Titulo("…")`, `BotonPrincipal("…", …)`, `Text("…")`, `Campo(…, "Etiqueta", …)` |
| Mensajes que aparecen abajo un momento («Datos guardados.») | En el `…VM` de la pantalla: `Mensajes.mostrar("…")` |
| Título de la barra superior | `ui/Navegacion.kt`, función `titulo()` |
| Textos de la barra inferior | `ui/Navegacion.kt`, `enum class Seccion` |
| Estados y fechas («Confirmada», «sábado 21 de noviembre») | `datos/Formato.kt` |
| Nombre de la app bajo el icono | `res/values/strings.xml`, `app_name` |
| Mensajes de error de la API («No hay mesa libre a esa hora»…) | Los escribe el servidor (repositorio Servidor), no la app |
| Nombres de campo en los errores («Teléfono» en vez de «phone») | `datos/Api.kt`, `ETIQUETAS` |

Al cambiar un texto:

- Cambia solo lo que va entre comillas. Lo que va con `$` o `${ … }` son datos:
  en `"Hola, ${usuario?.nombre…}"` solo se cambia «Hola,».
- Para encontrar un texto que ves en la app: en Android Studio, **Ctrl+Mayús+F**
  y escríbelo.
- Si el texto lleva comillas dobles, escríbelas como `\"`.

---

## 7. Nombre, icono y pantalla de carga

| Qué | Cómo |
|---|---|
| Nombre bajo el icono | `res/values/strings.xml`: `<string name="app_name">MesaLista</string>` |
| Icono de la app | En Android Studio: botón derecho en `res` > **New > Image Asset**, elige tu imagen (PNG o SVG) y deja el nombre `ic_launcher`. Sustituye los archivos de `mipmap-anydpi-v26/` y `drawable/ic_launcher_foreground.xml` |
| Fondo del icono y de la pantalla de carga | `res/values/colors.xml`, `bar` |
| Icono de la pantalla de carga | `res/values/themes.xml`, `windowSplashScreenAnimatedIcon` (por defecto, el mismo del icono) |
| Icono pequeño de los avisos push | `res/drawable/ic_aviso.xml` (blanco sobre transparente) |

---

## 8. Recetas rápidas

**Cambiar el texto de bienvenida.** `ui/acceso/Acceso.kt`, `BienvenidaPantalla`:
cambia el `Text("Reserva tu mesa sin llamar, …")`.

**Botón «Reservar mesa» más grande.** En `ui/inicio/Inicio.kt` no hace falta
tocar nada: es un `BotonPrincipal`. Cámbialo en `Componentes.kt`
(`heightIn(min = 60.dp)`) y cambian todos los botones principales a la vez.

**Tarjetas de reserva con otro color.** `ui/comun/Reservas.kt`, `TarjetaReserva`:
`containerColor = MaterialTheme.colorScheme.surfaceContainerHigh`. Pon otro color
del tema (`primaryContainer`, `secondaryContainer`…), no un color fijo, para que
funcione en modo oscuro.

**Más espacio entre bloques en todas las pantallas.** `Componentes.kt`,
`Pagina`: `Arrangement.spacedBy(16.dp)`.

**Cambiar el texto de una sección de la barra inferior.** `Navegacion.kt`,
`Reservas("reservas", "Mis reservas", …)`: cambia solo `"Mis reservas"`.

---

## 9. Qué no tocar

- Los parámetros de las pantallas (`irAReserva`, `alConfirmar`…) y las rutas
  (`"reserva/{id}"`…): `Navegacion.kt` los usa para ir de una a otra.
- Lo que hay en `datos/` salvo `Formato.kt` y `ETIQUETAS`: es la comunicación
  con el servidor, la sesión cifrada y los avisos push.
- El `applicationId` (`com.mesalista.cliente`) de `app/build.gradle.kts`: es
  el mismo que la app registrada en Firebase. Si cambia, dejan de llegar los avisos.

---

## 10. Publicar una versión nueva

1. Prueba los cambios en el emulador, en claro y en oscuro.
2. En `app/build.gradle.kts`, sube `versionCode` (de 1 a 2…) y cambia
   `versionName` («1.1»). Si `versionCode` no sube, el móvil no deja instalarla
   encima de la anterior.
3. Compila la versión firmada (README, «Versión para los móviles»):
   `./gradlew assembleRelease`. Hace falta la misma clave de firma
   (`keystore.properties` y `keystore/`) y `app/google-services.json`.
4. Copia `app/build/outputs/apk/release/app-release.apk` en `release/` con el
   nombre de la versión (`MesaLista-1.1.apk`), actualiza la tabla de la versión del
   README y súbelo a GitHub.
