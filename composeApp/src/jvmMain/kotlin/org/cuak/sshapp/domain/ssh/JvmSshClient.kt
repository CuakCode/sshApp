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

class JvmSshClient : SshClient {

    override suspend fun fetchMetrics(server: Server): Result<ServerMetrics> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        // SEGURIDAD: PromiscuousVerifier es solo para pruebas. En producción usar known_hosts.
        client.addHostKeyVerifier(PromiscuousVerifier())

        try {
            println("SSH: Conectando a ${server.ip}...")
            client.connect(server.ip, server.port)

            val cleanPassword = server.password?.trim() ?: ""

            // --- FASE 1: AUTENTICACIÓN ---
            if (!server.sshKeyPath.isNullOrBlank()) {
                val keyProvider: KeyProvider = client.loadKeys(server.sshKeyPath)
                client.authPublickey(server.username, keyProvider)
            } else {
                try {
                    client.authPassword(server.username, cleanPassword)
                } catch (e: Exception) {
                    println("SSH: authPassword falló. Probando Interactive...")
                    val kbi = AuthKeyboardInteractive(object : ChallengeResponseProvider {
                        override fun getSubmethods(): List<String> = emptyList()
                        override fun init(resource: Resource<*>?, name: String?, instruction: String?) {}
                        override fun shouldRetry(): Boolean = false
                        override fun getResponse(prompt: String?, echo: Boolean): CharArray {
                            return cleanPassword.toCharArray()
                        }
                    })
                    client.auth(server.username, kbi)
                }
            }

            // --- FASE 2: EJECUCIÓN ROBUSTA (UNO POR UNO) ---

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

            // 4. Temperatura (NUEVO)
            // Leemos thermal_zone0, que es estándar en Linux (millidegrees).
            // Si falla (común en Docker/VM), devuelve 0.0.
            val tempVal = try {
                val cmd = "cat /sys/class/thermal/thermal_zone0/temp"
                val raw = client.execOneCommand(cmd)
                // El valor suele venir en miligrados (ej: 45000 -> 45.0)
                (raw.toDoubleOrNull() ?: 0.0) / 1000.0
            } catch (e: Exception) {
                // Es normal que falle en Docker si no se monta /sys
                println("SSH Warn: Fallo al leer Temperatura (Normal en Docker/VM): ${e.message}")
                0.0
            }

            println("SSH: Métricas -> CPU: $cpuVal%, RAM: $ramVal%, Disk: $diskVal%, Temp: $tempVal°C")

            val metrics = ServerMetrics(
                cpuPercentage = cpuVal,
                ramPercentage = ramVal,
                diskUsage = listOf(diskVal),
                temperatures = mapOf("CPU" to tempVal)
            )

            Result.success(metrics)

        } catch (e: Exception) {
            println("SSH ERROR FINAL: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        } finally {
            if (client.isConnected) client.disconnect()
        }
    }

    override suspend fun executeCommand(server: Server, command: String): Result<String> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        client.addHostKeyVerifier(PromiscuousVerifier())
        try {
            client.connect(server.ip, server.port)
            // Auth simplificado para el ejemplo
            client.authPassword(server.username, server.password ?: "")
            val output = client.execOneCommand(command)
            Result.success(output)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (client.isConnected) client.disconnect()
        }
    }

    // --- FUNCIÓN HELPER PRIVADA ---
    private fun SSHClient.execOneCommand(command: String): String {
        val session = this.startSession()
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