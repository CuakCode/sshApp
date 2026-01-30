package org.cuak.sshapp.domain.ssh

import org.cuak.sshapp.models.ProcessInfo
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerMetrics

interface SshClient {

    suspend fun fetchMetrics(server: Server): Result<ServerMetrics>
    suspend fun executeCommand(server: Server, command: String): Result<String>
    suspend fun shutdown(server: Server): Result<Unit>

    suspend fun openTerminal(server: Server): Result<SshTerminalSession>
    suspend fun fetchProcesses(server: Server): Result<List<ProcessInfo>>
}