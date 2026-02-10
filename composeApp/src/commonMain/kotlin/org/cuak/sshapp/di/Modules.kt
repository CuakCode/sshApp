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
import org.cuak.sshapp.models.Server

val commonModule = module {
    // Base de datos
    single<ServerDatabase> { createDatabase(get()) }

    // Repositorio
    singleOf(::ServerRepository)

    // --- CORRECCIÓN: REGISTRAR CONNECTIVITY MANAGER ---
    // Esta es la línea que te falta y causa el error:
    factory { ConnectivityManager() }

    // HomeViewModel: Ahora recibe 2 parámetros (Repository, ConnectivityManager)
    // Por eso usamos get(), get()
    factory { HomeViewModel(get(), get()) }

    // ServerDetailViewModel
    factory { ServerDetailViewModel(get(), get()) }

    factory { (server: Server) ->
        FileManagerViewModel(
            server = server,
            sshClient = get(),       // Inyecta SshClient automáticamente
            localFileSystem = get()  // Inyecta LocalFileSystem automáticamente
        )
    }
}