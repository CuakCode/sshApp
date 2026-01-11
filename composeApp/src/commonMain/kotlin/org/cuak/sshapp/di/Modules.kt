package org.cuak.sshapp.di

import org.cuak.sshapp.ServerDatabase
import org.cuak.sshapp.database.createDatabase
import org.cuak.sshapp.database.DatabaseDriverFactory
import org.cuak.sshapp.repository.ServerRepository
import org.cuak.sshapp.ui.screens.HomeViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val commonModule = module {
    single<ServerDatabase> { createDatabase(get()) }
    singleOf(::ServerRepository)

    // Cambiamos factory por screenModel para Voyager
    factory { HomeViewModel(get()) }
    // Si creas un ServerDetailViewModel, regístralo aquí también
}