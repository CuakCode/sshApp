package org.cuak.sshapp.domain.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerMetrics
import java.util.concurrent.TimeUnit

class JvmSshClient : SshClient {

    override suspend fun fetchMetrics(server: Server): Result<ServerMetrics> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        // SEGURIDAD: Para un TFG, PromiscuousVerifier acepta cualquier clave del servidor (como la primera vez que conectas).
        // En producción real, deberías usar un verificador que compruebe known_hosts.
        client.addHostKeyVerifier(PromiscuousVerifier())

        try {
            // 1. Conexión
            client.connect(server.ip, server.port)

            // 2. Autenticación (Clave o Contraseña)
            if (!server.sshKeyPath.isNullOrBlank()) {
                val keyProvider: KeyProvider = client.loadKeys(server.sshKeyPath)
                client.authPublickey(server.username, keyProvider)
            } else {
                client.authPassword(server.username, server.password ?: "")
            }

            // 3. Comando eficiente: Obtiene CPU, RAM y Disco en una sola ejecución para latencia mínima
            // awk se usa para limpiar la salida y facilitar el parsing
            val cmdString = """
                top -bn1 | grep "Cpu(s)" | awk '{print $2 + $4}'
                free -m | awk 'NR==2{printf "%.2f", $3*100/$2 }'
                df -h / | awk 'NR==2{print $5}' | tr -d '%'
            """.trimIndent()

            val session = client.startSession()
            val cmd = session.exec(cmdString)

            // Leemos la salida
            val output = cmd.inputStream.reader().readText()
            cmd.join(5, TimeUnit.SECONDS)
            session.close()

            // 4. Parsing
            val lines = output.lines().filter { it.isNotBlank() }
            if (lines.size >= 3) {
                val cpu = lines[0].toDoubleOrNull() ?: 0.0
                val ram = lines[1].toDoubleOrNull() ?: 0.0
                val disk = lines[2].toDoubleOrNull() ?: 0.0

                // Simulación de temperatura (requiere lm-sensors instalado, difícil de estandarizar)
                val tempMap = mapOf("Core 0" to 45.0)

                val metrics = ServerMetrics(
                    cpuPercentage = cpu,
                    ramPercentage = ram,
                    diskUsage = listOf(disk),
                    temperatures = tempMap
                )
                Result.success(metrics)
            } else {
                Result.failure(Exception("Formato de respuesta inesperado: $output"))
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
        // Lógica similar para terminal (conectar, auth, exec, return output)
        Result.success("Terminal no implementada en este snippet")
    }
}