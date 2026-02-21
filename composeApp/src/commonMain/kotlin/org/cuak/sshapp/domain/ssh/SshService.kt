package org.cuak.sshapp.domain.ssh

import org.cuak.sshapp.models.ProcessInfo
import org.cuak.sshapp.models.Device
import org.cuak.sshapp.models.ServerMetrics
import org.cuak.sshapp.models.SftpFile

interface SshClient {

    suspend fun fetchMetrics(device: Device): Result<ServerMetrics>
    suspend fun executeCommand(device: Device, command: String): Result<String>
    suspend fun shutdown(device: Device): Result<Unit>

    suspend fun openTerminal(device: Device): Result<SshTerminalSession>
    suspend fun fetchProcesses(device: Device): Result<List<ProcessInfo>>
    suspend fun listRemoteFiles(device: Device, path: String): Result<List<SftpFile>>

    suspend fun uploadFile(
        device: Device,
        localPath: String,
        remotePath: String,
        onProgress: (Float) -> Unit // Float 0.0 a 1.0
    ): Result<Unit>

    suspend fun downloadFile(
        device: Device,
        remotePath: String,
        localPath: String,
        onProgress: (Float) -> Unit // Float 0.0 a 1.0
    ): Result<Unit>
}