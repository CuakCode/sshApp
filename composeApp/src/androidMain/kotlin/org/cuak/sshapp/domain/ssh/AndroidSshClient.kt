package org.cuak.sshapp.domain.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.userauth.method.AuthKeyboardInteractive
import net.schmizz.sshj.userauth.method.ChallengeResponseProvider
import net.schmizz.sshj.userauth.password.Resource
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.cuak.sshapp.models.DeviceType
import org.cuak.sshapp.models.PortInfo
import org.cuak.sshapp.models.ProcessInfo
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerMetrics
import java.io.File
import java.io.OutputStream
import java.security.Security
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

class AndroidSshClient : SshClient {

    companion object {
        init {
            // --- BLOQUE CRÍTICO PARA ANDROID ---
            // Esto es lo único que diferencia la configuración base respecto a JVM.
            // Registramos BouncyCastle para tener los algoritmos de encriptación necesarios.
            try {
                Security.removeProvider("BC")
                Security.addProvider(BouncyCastleProvider())
                SecurityUtils.setSecurityProvider("BC")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- 1. MONITOR (MÉTRICAS) ---
    // Lógica idéntica a JvmSshClient
    override suspend fun fetchMetrics(server: Server): Result<ServerMetrics> = withContext(Dispatchers.IO) {
        // Usamos el constructor por defecto. Si BouncyCastle está bien registrado arriba,
        // no necesitamos tocar userAuthFactories manualmente.
        val client = SSHClient()
        try {
            client.connectAndAuthenticate(server)

            // COMANDO CPU (Copiado de JvmSshClient)
            val cpuCmd = if (server.type == DeviceType.CAMERA) {
                "top -bn1 | grep '^CPU' | tr -d '%' | awk '{print \$2 + \$4}'"
            } else {
                "top -bn1 | grep -i 'Cpu(s)' | awk '{print \$2 + \$4}'"
            }

            val cpuVal = try {
                val output = client.execOneCommand(cpuCmd)
                val cleanLine = output.lines().firstOrNull { it.any { c -> c.isDigit() } } ?: "0.0"
                cleanLine.trim().toDoubleOrNull() ?: 0.0
            } catch (e: Exception) { 0.0 }

            // COMANDO RAM
            val ramCmd = if (server.type == DeviceType.CAMERA) {
                "free | awk 'NR==2{printf \"%.2f\", \$3*100/\$2 }'"
            } else {
                "free -m | awk 'NR==2{printf \"%.2f\", \$3*100/\$2 }'"
            }

            val ramVal = try {
                val output = client.execOneCommand(ramCmd)
                output.trim().toDoubleOrNull() ?: 0.0
            } catch (e: Exception) { 0.0 }

            // DISCO
            val diskVal = try {
                val cmd = "df -P / | awk 'NR==2{print \$5}' | tr -d '%'"
                client.execOneCommand(cmd).toDoubleOrNull() ?: 0.0
            } catch (e: Exception) { 0.0 }

            // TEMPERATURA
            val tempVal = try {
                val cmd = "cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null || cat /sys/class/hwmon/hwmon0/temp1_input 2>/dev/null"
                val raw = client.execOneCommand(cmd)
                (raw.toDoubleOrNull() ?: 0.0) / 1000.0
            } catch (e: Exception) { 0.0 }

            // PUERTOS
            val ports = try {
                val cmd = "netstat -nlp | grep LISTEN"
                val output = client.execOneCommand(cmd)
                parsePorts(output)
            } catch (e: Exception) { emptyList() }

            val metrics = ServerMetrics(
                cpuPercentage = cpuVal,
                ramPercentage = ramVal,
                diskUsage = listOf(diskVal),
                temperatures = if (tempVal > 0) mapOf("CPU" to tempVal) else emptyMap(),
                openPorts = ports
            )
            Result.success(metrics)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            if (client.isConnected) client.disconnect()
        }
    }

    // --- 2. TERMINAL ---
    override suspend fun openTerminal(server: Server): Result<SshTerminalSession> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        try {
            client.connectAndAuthenticate(server)

            val session = client.startSession()
            session.allocateDefaultPTY()
            val shell = session.startShell()

            Result.success(AndroidTerminalSession(client, session, shell))
        } catch (e: Exception) {
            e.printStackTrace()
            try { client.disconnect() } catch (ignore: Exception) {}
            Result.failure(e)
        }
    }

    // --- 3. PROCESOS ---
    override suspend fun fetchProcesses(server: Server): Result<List<ProcessInfo>> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        try {
            client.connectAndAuthenticate(server)

            val processes = if (server.type == DeviceType.CAMERA) {
                val cmd = "top -b -n 1"
                val output = client.execOneCommand(cmd)
                parseBusyBoxTop(output)
            } else {
                val cmd = "ps -e -o pid,user,%cpu,%mem,comm --sort=-%cpu | head -n 50"
                val output = client.execOneCommand(cmd)
                parseLinuxPs(output)
            }
            Result.success(processes)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (client.isConnected) client.disconnect()
        }
    }

    // --- 4. COMANDOS ---
    override suspend fun executeCommand(server: Server, command: String): Result<String> = withContext(Dispatchers.IO) {
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

    // --- 5. APAGADO ---
    override suspend fun shutdown(server: Server): Result<Unit> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        try {
            client.connectAndAuthenticate(server)
            val session = client.startSession()
            try {
                val cmd = session.exec("sudo -S -p '' poweroff")
                val password = server.password?.trim() ?: ""
                cmd.outputStream.use { it.write((password + "\n").toByteArray()) }
                cmd.join(2, TimeUnit.SECONDS)
                Result.success(Unit)
            } finally {
                session.close()
            }
        } catch (e: Exception) {
            Result.success(Unit)
        } finally {
            if (client.isConnected) client.disconnect()
        }
    }

    // ========================================================================
    // --- CLASE INTERNA DE SESIÓN (Copia exacta de JvmTerminalSession) ---
    // ========================================================================
    private class AndroidTerminalSession(
        private val client: SSHClient,
        private val session: Session,
        private val shell: Session.Shell
    ) : SshTerminalSession {

        private val outputStream: OutputStream = shell.outputStream

        override val output: Flow<String> = flow {
            val reader = shell.inputStream
            val buffer = ByteArray(4096)
            try {
                while (coroutineContext.isActive && shell.isOpen) {
                    if (reader.available() > 0) {
                        val read = reader.read(buffer)
                        if (read == -1) break
                        val text = String(buffer, 0, read)
                        emit(text)
                    } else {
                        delay(10)
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

    // ========================================================================
    // --- HELPERS (Copia exacta de JvmSshClient) ---
    // ========================================================================

    private fun SSHClient.connectAndAuthenticate(server: Server) {
        addHostKeyVerifier(PromiscuousVerifier())
        connect(server.ip, server.port)

        val cleanPassword = server.password?.trim() ?: ""

        if (!server.sshKeyPath.isNullOrBlank()) {
            val keyFile = File(server.sshKeyPath)
            if (keyFile.exists()) {
                val keyProvider: KeyProvider = loadKeys(keyFile.absolutePath)
                authPublickey(server.username, keyProvider)
            } else {
                // Fallback a password si la key existe pero falla o no se carga
                tryAuthPassword(server.username, cleanPassword)
            }
        } else {
            tryAuthPassword(server.username, cleanPassword)
        }
    }

    private fun SSHClient.tryAuthPassword(username: String, pass: String) {
        try {
            authPassword(username, pass)
        } catch (e: Exception) {
            val kbi = AuthKeyboardInteractive(object : ChallengeResponseProvider {
                override fun getSubmethods() = emptyList<String>()
                override fun init(r: Resource<*>?, n: String?, i: String?) {}
                override fun shouldRetry() = false
                override fun getResponse(p: String?, e: Boolean) = pass.toCharArray()
            })
            auth(username, kbi)
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

    private fun parseLinuxPs(output: String): List<ProcessInfo> {
        val list = mutableListOf<ProcessInfo>()
        val lines = output.lines().drop(1)
        for (line in lines) {
            if (line.isBlank()) continue
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size >= 5) {
                try {
                    val commandName = parts.subList(4, parts.size).joinToString(" ")
                    if (commandName == "ps" || commandName == "head") continue
                    list.add(ProcessInfo(
                        pid = parts[0],
                        user = parts[1],
                        cpuUsage = parts[2].toDoubleOrNull() ?: 0.0,
                        memUsage = parts[3].toDoubleOrNull() ?: 0.0,
                        command = commandName
                    ))
                } catch (e: Exception) { }
            }
        }
        return list
    }

    private fun parseBusyBoxTop(output: String): List<ProcessInfo> {
        val list = mutableListOf<ProcessInfo>()
        val lines = output.lines()
        var headerFound = false
        for (line in lines) {
            if (line.contains("PID") && line.contains("COMMAND")) {
                headerFound = true
                continue
            }
            if (!headerFound || line.isBlank()) continue
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size >= 9) {
                try {
                    val commandName = parts.subList(8, parts.size).joinToString(" ")
                    if (commandName.contains("top")) continue
                    list.add(ProcessInfo(
                        pid = parts[0],
                        user = parts[2],
                        cpuUsage = parts[7].replace("%","").toDoubleOrNull() ?: 0.0,
                        memUsage = parts[5].replace("%","").toDoubleOrNull() ?: 0.0,
                        command = commandName
                    ))
                } catch (e: Exception) { }
            }
        }
        return list
    }

    private fun parsePorts(netstatOutput: String): List<PortInfo> {
        val list = mutableListOf<PortInfo>()
        netstatOutput.lines().forEach { line ->
            try {
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 4 && (parts[0] == "tcp" || parts[0] == "udp" || line.contains("LISTEN"))) {
                    val protocol = parts[0]
                    val addressParts = parts.find { it.contains(":") && it.split(":").size >= 2 }
                    val pidPart = parts.find { it.contains("/") }

                    if (addressParts != null) {
                        val portStr = addressParts.substringAfterLast(":")
                        val port = portStr.toIntOrNull()
                        val processName = pidPart?.substringAfter("/") ?: "?"

                        if (port != null && port > 0) {
                            list.add(PortInfo(port, protocol, processName))
                        }
                    }
                }
            } catch (e: Exception) { }
        }
        return list.distinctBy { it.port }.sortedBy { it.port }
    }
}