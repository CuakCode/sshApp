package org.cuak.sshapp.domain.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.method.AuthKeyboardInteractive // Importante
import net.schmizz.sshj.userauth.password.PasswordFinder      // Importante
import net.schmizz.sshj.userauth.password.Resource            // Importante
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerMetrics
import java.util.concurrent.TimeUnit

class JvmSshClient : SshClient {

    override suspend fun fetchMetrics(server: Server): Result<ServerMetrics> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        // SEGURIDAD: En producción usar known_hosts. Para TFG/Dev: PromiscuousVerifier
        client.addHostKeyVerifier(PromiscuousVerifier())

        try {
            client.connect(server.ip, server.port)

            // --- LÓGICA DE AUTENTICACIÓN MEJORADA ---
            if (!server.sshKeyPath.isNullOrBlank()) {
                val keyProvider: KeyProvider = client.loadKeys(server.sshKeyPath)
                client.authPublickey(server.username, keyProvider)
            } else {
                // Definimos el proveedor de contraseñas para el modo interactivo
                val passwordFinder = object : PasswordFinder {
                    override fun reqPassword(resource: Resource<*>?): CharArray {
                        return (server.password ?: "").toCharArray()
                    }
                    override fun shouldRetry(resource: Resource<*>?): Boolean = false
                }

                try {
                    // Intento 1: Autenticación estándar por contraseña
                    client.authPassword(server.username, server.password ?: "")
                } catch (e: Exception) {
                    // Intento 2: Si falla, probamos Keyboard Interactive
                    // Esto soluciona el error "Exhausted available authentication methods" en Alpine/Ubuntu
                    client.authInteractive(server.username, passwordFinder)
                }
            }
            // ----------------------------------------

            val cmdString = """
                top -bn1 | grep "Cpu(s)" | awk '{print $2 + $4}'
                free -m | awk 'NR==2{printf "%.2f", $3*100/$2 }'
                df -h / | awk 'NR==2{print $5}' | tr -d '%'
            """.trimIndent()

            val session = client.startSession()
            val cmd = session.exec(cmdString)

            val output = cmd.inputStream.reader().readText()
            cmd.join(5, TimeUnit.SECONDS)
            session.close()

            val lines = output.lines().filter { it.isNotBlank() }
            if (lines.size >= 3) {
                val cpu = lines[0].toDoubleOrNull() ?: 0.0
                val ram = lines[1].toDoubleOrNull() ?: 0.0
                val disk = lines[2].toDoubleOrNull() ?: 0.0
                val tempMap = mapOf("Core 0" to 45.0)

                val metrics = ServerMetrics(
                    cpuPercentage = cpu,
                    ramPercentage = ram,
                    diskUsage = listOf(disk),
                    temperatures = tempMap
                )
                Result.success(metrics)
            } else {
                Result.failure(Exception("Formato inesperado: $output"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            if (client.isConnected) {
                client.disconnect()
            }
        }
    }

    override suspend fun executeCommand(server: Server, command: String): Result<String> = withContext(Dispatchers.IO) {
        Result.success("Terminal no implementada")
    }
}