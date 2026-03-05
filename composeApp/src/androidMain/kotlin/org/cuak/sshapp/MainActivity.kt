package org.cuak.sshapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import org.cuak.sshapp.repository.SettingsRepository
import org.koin.android.ext.android.inject
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private val settingsRepository: SettingsRepository by inject()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        applyLocale()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        applyLocale()

        requestStoragePermissions()

        setContent {
            App()
        }
    }

    private fun applyLocale() {
        val currentLang = settingsRepository.settings.value.language

        val localeToApply = when (currentLang) {
            "es" -> Locale.Builder().setLanguage("es").setRegion("ES").build()
            "en" -> Locale.Builder().setLanguage("en").setRegion("US").build()
            else -> {
                val sysLang = Locale.getDefault().language
                if (sysLang == "es") {
                    Locale.Builder().setLanguage("es").setRegion("ES").build()
                } else {
                    Locale.Builder().setLanguage("en").setRegion("US").build()
                }
            }
        }

        Locale.setDefault(localeToApply)

        val configuration = Configuration(resources.configuration)
        configuration.setLocale(localeToApply)

        configuration.setLayoutDirection(localeToApply)

        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = String.format("package:%s", applicationContext.packageName).toUri()
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        } else {
            val readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

            if (readPermission != PackageManager.PERMISSION_GRANTED || writePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }
}