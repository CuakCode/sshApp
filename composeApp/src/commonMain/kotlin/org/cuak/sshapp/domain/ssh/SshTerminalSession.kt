package org.cuak.sshapp.domain.ssh

import kotlinx.coroutines.flow.Flow

interface SshTerminalSession {
    
    val output: Flow<String>

    
    suspend fun write(input: String)

    
    fun close()
}