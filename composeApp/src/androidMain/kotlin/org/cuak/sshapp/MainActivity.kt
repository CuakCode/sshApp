package org.cuak.sshapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // NOTA: Ya no llamamos a initKoin aquí.
        // Se encarga SSHApplication automáticamente.

        setContent {
            App()
        }
    }
}