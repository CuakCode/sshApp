package org.cuak.sshapp.domain.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.method.AuthKeyboardInteractive
import net.schmizz.sshj.userauth.method.ChallengeResponseProvider
import net.schmizz.sshj.userauth.password.Resource
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerMetrics
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import net.schmizz.sshj.connection.channel.direct.Session
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

class JvmSshClient : SshClient {

    override suspend fun fetchMetrics(server: Server): Result<ServerMetrics> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        try {
            client.connectAndAuthenticate(server)

            // 1. CPU
            val cpuVal = try {
                val cmd = "top -bn1 | grep -i 'Cpu(s)' | awk '{print \$2 + \$4}'"
                client.execOneCommand(cmd).toDoubleOrNull() ?: 0.0
            } catch (e: Exception) {
                println("SSH Warn: Fallo al leer CPU: ${e.message}")
                0.0
            }

            // 2. RAM
            val ramVal = try {
                val cmd = "free -m | awk 'NR==2{printf \"%.2f\", \$3*100/\$2 }'"
                client.execOneCommand(cmd).toDoubleOrNull() ?: 0.0
            } catch (e: Exception) {
                println("SSH Warn: Fallo al leer RAM: ${e.message}")
                0.0
            }

            // 3. Disco
            val diskVal = try {
                val cmd = "df -P / | awk 'NR==2{print \$5}' | tr -d '%'"
                client.execOneCommand(cmd).toDoubleOrNull() ?: 0.0
            } catch (e: Exception) {
                println("SSH Warn: Fallo al leer Disco: ${e.message}")
                0.0
            }

            // 4. Temperatura
            val tempVal = try {
                val cmd = "cat /sys/class/thermal/thermal_zone0/temp"
                val raw = client.execOneCommand(cmd)
                (raw.toDoubleOrNull() ?: 0.0) / 1000.0
            } catch (e: Exception) {
                println("SSH Warn: Fallo al leer Temperatura: ${e.message}")
                0.0
            }

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

    override suspend fun executeCommand(server: Server, command: String): Result<String> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        try {
            client.connectAndAuthenticate(server)
            val output = client.execOneCommand(command)
            Result.success(output)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (client.isConnected) client.disconnect()
        }
    }

    private fun SSHClient.connectAndAuthenticate(server: Server) {
        // SEGURIDAD: PromiscuousVerifier acepta cualquier host key. En prod usar known_hosts.
        addHostKeyVerifier(PromiscuousVerifier())
        connect(server.ip, server.port)

        val cleanPassword = server.password?.trim() ?: ""

        if (!server.sshKeyPath.isNullOrBlank()) {
            val keyProvider: KeyProvider = loadKeys(server.sshKeyPath)
            authPublickey(server.username, keyProvider)
        } else {
            try {
                // Intento estándar
                authPassword(server.username, cleanPassword)
            } catch (e: Exception) {
                // Fallback: Keyboard Interactive (común en Ubuntu/Alpine recientes)
                val kbi = AuthKeyboardInteractive(object : ChallengeResponseProvider {
                    override fun getSubmethods(): List<String> = emptyList()
                    override fun init(resource: Resource<*>?, name: String?, instruction: String?) {}
                    override fun shouldRetry(): Boolean = false
                    override fun getResponse(prompt: String?, echo: Boolean): CharArray {
                        return cleanPassword.toCharArray()
                    }
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

    override suspend fun shutdown(server: Server): Result<Unit> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        try {
            client.connectAndAuthenticate(server)

            val session = client.startSession()
            try {
                val shutdownCommand = "sudo -S -p '' poweroff 2>/dev/null || poweroff 2>/dev/null || kill -s TERM 1"

                val cmd = session.exec(shutdownCommand)

                // Inyectamos la contraseña por si acaso se ejecuta la parte de 'sudo'
                val password = server.password?.trim() ?: ""
                cmd.outputStream.use { out ->
                    out.write((password + "\n").toByteArray())
                    out.flush()
                }

                // Esperamos un momento. Si es un container, la conexión morirá casi al instante.
                cmd.join(2, TimeUnit.SECONDS)

                Result.success(Unit)
            } finally {
                session.close()
            }
        } catch (e: Exception) {
            // En Docker, al matar el PID 1, la conexión SSH se corta abruptamente (Broken pipe).
            // Esto técnicamente es una excepción, pero significa que el apagado funcionó.
            // Lo tratamos como éxito si el mensaje sugiere cierre de conexión.
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("broken pipe") || msg.contains("stream closed") || msg.contains("connection reset")) {
                Result.success(Unit)
            } else {
                Result.failure(e)
            }
        } finally {
            if (client.isConnected) client.disconnect()
        }
    }

    override suspend fun openTerminal(server: Server): Result<SshTerminalSession> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        try {
            // Reutilizamos la lógica de conexión existente (extraer a un método privado si es necesario)
            client.addHostKeyVerifier(PromiscuousVerifier())
            client.connect(server.ip, server.port)

            // Lógica de autenticación (Copiada de tu implementación actual o extraída a función helper)
            val cleanPassword = server.password?.trim() ?: ""
            if (!server.sshKeyPath.isNullOrBlank()) {
                val keyProvider = client.loadKeys(server.sshKeyPath)
                client.authPublickey(server.username, keyProvider)
            } else {
                client.authPassword(server.username, cleanPassword)
            }

            // Iniciamos la sesión y la shell
            val session = client.startSession()
            session.allocateDefaultPTY() // Pseudo-terminal para que comandos interactivos como 'top' o 'sudo' funcionen
            val shell = session.startShell()

            Result.success(JvmTerminalSession(client, session, shell))
        } catch (e: Exception) {
            e.printStackTrace()
            if (client.isConnected) client.disconnect()
            Result.failure(e)
        }
    }

    // Clase interna para manejar la sesión específica de SSHJ
    private class JvmTerminalSession(
        private val client: SSHClient,
        private val session: Session,
        private val shell: Session.Shell
    ) : SshTerminalSession {

        private val outputStream: OutputStream = shell.outputStream

        override val output: Flow<String> = flow {
            val reader = shell.inputStream
            val buffer = ByteArray(1024)
            try {
                while (coroutineContext.isActive && shell.isOpen) {
                    if (reader.available() > 0) {
                        val read = reader.read(buffer)
                        if (read == -1) break
                        val text = String(buffer, 0, read)
                        emit(text)
                    } else {
                        // Pequeña pausa para no saturar la CPU si no hay datos
                        kotlinx.coroutines.delay(50)
                    }
                }
            } catch (e: Exception) {
                // Manejo de desconexión
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}