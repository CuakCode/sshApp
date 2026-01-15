package org.cuak.sshapp.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}, platformModules: List<Module> = emptyList()) =
    startKoin {
        appDeclaration()
        modules(commonModule + platformModules)
    }