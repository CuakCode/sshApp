package org.cuak.sshapp.domain.ssh

import org.cuak.sshapp.models.ProcessInfo
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerMetrics
import org.cuak.sshapp.models.SftpFile

interface SshClient {

    suspend fun fetchMetrics(server: Server): Result<ServerMetrics>
    suspend fun executeCommand(server: Server, command: String): Result<String>
    suspend fun shutdown(server: Server): Result<Unit>

    suspend fun openTerminal(server: Server): Result<SshTerminalSession>
    suspend fun fetchProcesses(server: Server): Result<List<ProcessInfo>>
    suspend fun listRemoteFiles(server: Server, path: String): Result<List<SftpFile>> {
        return Result.failure(Exception("Not implemented yet"))
    }

    suspend fun uploadFile(server: Server, localPath: String, remotePath: String): Result<Unit> {
        return Result.failure(Exception("Not implemented yet"))
    }

    suspend fun downloadFile(server: Server, remotePath: String, localPath: String): Result<Unit> {
        return Result.failure(Exception("Not implemented yet"))
    }
}