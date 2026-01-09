package org.cuak.sshapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.cuak.sshapp.database.DatabaseDriverFactory
import org.cuak.sshapp.database.createDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val driverFactory = DatabaseDriverFactory(applicationContext)
        val database = createDatabase(driverFactory)

        setContent {
            App(database = database)
        }
    }
}
/*
@Preview
@Composable
fun AppAndroidPreview() {
    App()
}*/