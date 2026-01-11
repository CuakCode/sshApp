package org.cuak.sshapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.cuak.sshapp.database.DatabaseDriverFactory
import org.cuak.sshapp.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initKoin(
            appDeclaration = { androidContext(this@MainActivity) },
            platformModules = listOf(module { single { DatabaseDriverFactory(applicationContext) } })
        )

        setContent {
            App()
        }
    }
}