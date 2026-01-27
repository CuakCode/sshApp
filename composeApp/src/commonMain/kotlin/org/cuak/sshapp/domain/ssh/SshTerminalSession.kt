package org.cuak.sshapp.domain.ssh

import kotlinx.coroutines.flow.Flow

interface SshTerminalSession {
    // Flujo de salida del servidor (lo que recibimos)
    val output: Flow<String>

    // Método para enviar texto o comandos al servidor
    suspend fun write(input: String)

    // Cerrar la sesión
    fun close()
}