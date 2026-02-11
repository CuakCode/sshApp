package org.cuak.sshapp

import android.app.Application
import org.cuak.sshapp.database.AndroidDatabaseDriverFactory
import org.cuak.sshapp.database.DatabaseDriverFactory
import org.cuak.sshapp.di.initKoin
import org.cuak.sshapp.domain.files.AndroidLocalFileSystem
import org.cuak.sshapp.domain.files.LocalFileSystem
import org.cuak.sshapp.domain.ssh.AndroidSshClient
import org.cuak.sshapp.domain.ssh.SshClient
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.dsl.module

class SSHApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Iniciamos Koin una única vez aquí.
        // Esto evita el crash al rotar la pantalla.
        initKoin(
            appDeclaration = {
                androidLogger()
                androidContext(this@SSHApplication)
            },
            platformModules = listOf(module {
                // Definimos las implementaciones específicas de Android aquí
                single<DatabaseDriverFactory> { AndroidDatabaseDriverFactory(androidContext()) }
                single<SshClient> { AndroidSshClient() }
                single<LocalFileSystem> { AndroidLocalFileSystem(androidContext()) }
            })
        )
    }
}