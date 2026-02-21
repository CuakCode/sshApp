package org.cuak.sshapp.di

import org.cuak.sshapp.ServerDatabase
import org.cuak.sshapp.database.createDatabase
import org.cuak.sshapp.repository.ServerRepository
import org.cuak.sshapp.ui.screens.viewModels.HomeViewModel
import org.cuak.sshapp.ui.screens.viewModels.ServerDetailViewModel
// IMPORTANTE: Asegúrate de importar tu ConnectivityManager
import org.cuak.sshapp.network.ConnectivityManager
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.cuak.sshapp.ui.screens.viewModels.FileManagerViewModel
import org.cuak.sshapp.models.Device // Cambiado de Server a Device
import org.cuak.sshapp.repository.SettingsRepository
import org.cuak.sshapp.ui.screens.viewModels.SettingsViewModel

val commonModule = module {
    // Base de datos
    single<ServerDatabase> { createDatabase(get()) }

    // Repositorio
    singleOf(::ServerRepository)

    singleOf(::SettingsRepository)

    // --- CORRECCIÓN: REGISTRAR CONNECTIVITY MANAGER ---
    single { ConnectivityManager() }

    factory { SettingsViewModel(get()) }

    // HomeViewModel: Ahora recibe 2 parámetros (Repository, ConnectivityManager)
    factory { HomeViewModel(get(), get(), get()) }

    // ServerDetailViewModel
    factory { ServerDetailViewModel(get(), get(), get()) }

    // --- ACTUALIZADO A DEVICE ---
    factory { (device: Device) ->
        FileManagerViewModel(
            device = device,         // Asignamos el device
            sshClient = get(),       // Inyecta SshClient automáticamente
            localFileSystem = get()  // Inyecta LocalFileSystem automáticamente
        )
    }
}