import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firebase (avisos push) solo si está app/google-services.json: sin él, la app
// compila y funciona igual, pero sin avisos.
val conFirebase = file("google-services.json").exists()
if (conFirebase) {
    apply(plugin = "com.google.gms.google-services")
}

// Clave de firma de la versión release: keystore.properties (no se sube a Git).
val firma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

android {
    namespace = "com.mesalista.cliente"
    compileSdk = 35

    defaultConfig {
        // El mismo paquete que la app registrada en Firebase (google-services.json).
        applicationId = "com.mesalista.cliente"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("boolean", "CON_FIREBASE", conFirebase.toString())
    }

    signingConfigs {
        create("release") {
            if (firma.isNotEmpty()) {
                storeFile = rootProject.file(firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Desarrollo: la API de este ordenador (HTTP solo hacia 10.0.2.2, ver src/debug).
            val api = providers.gradleProperty("mesalista.api.debug").get().trimEnd('/')
            buildConfigField("String", "API", "\"$api\"")
        }
        release {
            val api = providers.gradleProperty("mesalista.api").get().trimEnd('/')
            require(api.startsWith("https://")) { "mesalista.api debe empezar por https:// (RS-01)" }
            buildConfigField("String", "API", "\"$api\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (firma.isNotEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Llamadas a la API (HTTPS) y tareas en segundo plano.
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
}
