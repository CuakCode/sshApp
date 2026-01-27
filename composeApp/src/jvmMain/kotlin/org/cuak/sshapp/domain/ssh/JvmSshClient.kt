package org.cuak.sshapp.domain.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.method.AuthKeyboardInteractive
import net.schmizz.sshj.userauth.method.ChallengeResponseProvider
import net.schmizz.sshj.userauth.password.Resource
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerMetrics
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class JvmSshClient : SshClient {

    // --- 1. MÉTODO PARA MONITOR (MÉTRICAS) ---
    override suspend fun fetchMetrics(server: Server): Result<ServerMetrics> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        try {
            client.connectAndAuthenticate(server)

            // CPU
            val cpuVal = try {
                val cmd = "top -bn1 | grep -i 'Cpu(s)' | awk '{print \$2 + \$4}'"
                client.execOneCommand(cmd).toDoubleOrNull() ?: 0.0
            } catch (e: Exception) { 0.0 }

            // RAM
            val ramVal = try {
                val cmd = "free -m | awk 'NR==2{printf \"%.2f\", \$3*100/\$2 }'"
                client.execOneCommand(cmd).toDoubleOrNull() ?: 0.0
            } catch (e: Exception) { 0.0 }

            // Disco
            val diskVal = try {
                val cmd = "df -P / | awk 'NR==2{print \$5}' | tr -d '%'"
                client.execOneCommand(cmd).toDoubleOrNull() ?: 0.0
            } catch (e: Exception) { 0.0 }

            // Temperatura
            val tempVal = try {
                // Intentamos varias rutas comunes de temperatura
                val cmd = "cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null || cat /sys/class/hwmon/hwmon0/temp1_input 2>/dev/null"
                val raw = client.execOneCommand(cmd)
                (raw.toDoubleOrNull() ?: 0.0) / 1000.0
            } catch (e: Exception) { 0.0 }

            val metrics = ServerMetrics(
                cpuPercentage = cpuVal,
                ramPercentage = ramVal,
                diskUsage = listOf(diskVal),
                temperatures = mapOf("CPU" to tempVal)
            )

            Result.success(metrics)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            if (client.isConnected) client.disconnect()
        }
    }

    // --- 2. MÉTODO PARA TERMINAL ---
    override suspend fun openTerminal(server: Server): Result<SshTerminalSession> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        try {
            client.connectAndAuthenticate(server)

            val session = client.startSession()
            session.allocateDefaultPTY() // IMPORTANTE: Pseudo-terminal
            val shell = session.startShell()

            Result.success(JvmTerminalSession(client, session, shell))
        } catch (e: Exception) {
            e.printStackTrace()
            if (client.isConnected) client.disconnect()
            Result.failure(e)
        }
    }

    // --- 3. MÉTODO PARA APAGAR ---
    override suspend fun executeCommand(server: Server, command: String): Result<String> = withContext(Dispatchers.IO) {
        // Implementación simple para comandos one-shot
        val client = SSHClient()
        try {
            client.connectAndAuthenticate(server)
            Result.success(client.execOneCommand(command))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (client.isConnected) client.disconnect()
        }
    }

    override suspend fun shutdown(server: Server): Result<Unit> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        try {
            client.connectAndAuthenticate(server)
            val session = client.startSession()
            try {
                // Comando agresivo de apagado
                val cmd = session.exec("sudo -S -p '' poweroff")
                val password = server.password?.trim() ?: ""
                cmd.outputStream.use { it.write((password + "\n").toByteArray()) }
                cmd.join(2, TimeUnit.SECONDS)
                Result.success(Unit)
            } finally {
                session.close()
            }
        } catch (e: Exception) {
            // Ignoramos error de desconexión abrupta (es normal al apagar)
            Result.success(Unit)
        } finally {
            if (client.isConnected) client.disconnect()
        }
    }

    // --- CLASE INTERNA DE SESIÓN ---
    private class JvmTerminalSession(
        private val client: SSHClient,
        private val session: Session,
        private val shell: Session.Shell
    ) : SshTerminalSession {

        private val outputStream: OutputStream = shell.outputStream

        override val output: Flow<String> = flow {
            val reader = shell.inputStream
            val buffer = ByteArray(4096) // Buffer más grande
            try {
                while (coroutineContext.isActive && shell.isOpen) {
                    if (reader.available() > 0) {
                        val read = reader.read(buffer)
                        if (read == -1) break
                        val text = String(buffer, 0, read)
                        emit(text)
                    } else {
                        delay(10) // Polling rápido
                    }
                }
            } catch (e: Exception) {
                // Fin de sesión
            }
        }.flowOn(Dispatchers.IO)

        override suspend fun write(input: String) = withContext(Dispatchers.IO) {
            try {
                outputStream.write(input.toByteArray())
                outputStream.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun close() {
            try {
                session.close()
                client.disconnect()
            } catch (e: Exception) {}
        }
    }

    // --- HELPERS PRIVADOS ---
    private fun SSHClient.connectAndAuthenticate(server: Server) {
        addHostKeyVerifier(PromiscuousVerifier())
        connect(server.ip, server.port)

        val cleanPassword = server.password?.trim() ?: ""
        if (!server.sshKeyPath.isNullOrBlank()) {
            val keyProvider: KeyProvider = loadKeys(server.sshKeyPath)
            authPublickey(server.username, keyProvider)
        } else {
            try {
                authPassword(server.username, cleanPassword)
            } catch (e: Exception) {
                val kbi = AuthKeyboardInteractive(object : ChallengeResponseProvider {
                    override fun getSubmethods() = emptyList<String>()
                    override fun init(r: Resource<*>?, n: String?, i: String?) {}
                    override fun shouldRetry() = false
                    override fun getResponse(p: String?, e: Boolean) = cleanPassword.toCharArray()
                })
                auth(server.username, kbi)
            }
        }
    }

    private fun SSHClient.execOneCommand(command: String): String {
        val session = startSession()
        return try {
            val cmd = session.exec(command)
            val output = cmd.inputStream.reader().readText().trim()
            cmd.join(5, TimeUnit.SECONDS)
            output
        } finally {
            session.close()
        }
    }
}