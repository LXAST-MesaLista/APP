package com.mesalista.cliente

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mesalista.cliente.datos.Enlaces
import com.mesalista.cliente.ui.RaizApp
import com.mesalista.cliente.ui.tema.TemaMesaLista

/**
 * La única actividad: todas las pantallas son de Compose (ui/). Recibe
 * también los enlaces mesalista://reserva/… (vuelta de PayPal) y los avisos
 * push que se tocan (ver datos/Enlaces.kt).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) Enlaces.recibir(intent)
        setContent {
            TemaMesaLista {
                RaizApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Enlaces.recibir(intent)
    }
}
