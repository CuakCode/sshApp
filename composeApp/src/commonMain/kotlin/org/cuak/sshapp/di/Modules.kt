package org.cuak.sshapp.di

import org.cuak.sshapp.ServerDatabase
import org.cuak.sshapp.database.createDatabase
import org.cuak.sshapp.repository.ServerRepository
import org.cuak.sshapp.ui.screens.viewModels.HomeViewModel
import org.cuak.sshapp.ui.screens.viewModels.ServerDetailViewModel
import org.cuak.sshapp.network.ConnectivityManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.cuak.sshapp.ui.screens.viewModels.FileManagerViewModel
import org.cuak.sshapp.models.Device
import org.cuak.sshapp.repository.SettingsRepository
import org.cuak.sshapp.ui.screens.viewModels.SettingsViewModel

// Importamos nuestra interfaz y la clase expect
import org.cuak.sshapp.domain.security.EncryptionService
import org.cuak.sshapp.domain.security.PlatformEncryptionService

val commonModule = module {
    // 1. Base de datos
    single<ServerDatabase> { createDatabase(get()) }

    // 2. SEGURIDAD: Inyectamos directamente la clase multiplataforma
    single<EncryptionService> { PlatformEncryptionService() }

    // 3. Repositorios
    singleOf(::ServerRepository) // Automáticamente cogerá el EncryptionService de arriba
    singleOf(::SettingsRepository)

    // 4. Servicios
    single { ConnectivityManager() }

    // 5. ViewModels
    factory { SettingsViewModel(get()) }
    factory { HomeViewModel(get(), get(), get()) }
    factory { ServerDetailViewModel(get(), get(), get()) }

    factory { (device: Device) ->
        FileManagerViewModel(
            device = device,
            sshClient = get(),
            localFileSystem = get()
        )
    }
}