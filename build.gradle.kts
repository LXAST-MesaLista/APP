// Versiones de las herramientas de compilación (las mismas que la app anterior).
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    // Firebase: lee app/google-services.json (avisos push con FCM).
    id("com.google.gms.google-services") version "4.4.2" apply false
}
