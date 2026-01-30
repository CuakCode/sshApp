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
import org.cuak.sshapp.models.DeviceType
import org.cuak.sshapp.models.PortInfo
import org.cuak.sshapp.models.ProcessInfo
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerMetrics
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.collections.emptyList
import kotlin.coroutines.coroutineContext


class JvmSshClient : SshClient {

    // --- 1. MÉTODO PARA MONITOR (MÉTRICAS) ---
    override suspend fun fetchMetrics(server: Server): Result<ServerMetrics> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        try {
            client.connectAndAuthenticate(server)

            // --- 1. COMANDO DE CPU CORREGIDO ---
            val cpuCmd = if (server.type == DeviceType.CAMERA) {
                // ESTRATEGIA CÁMARA (Yi Hack / BusyBox)
                // Tu log confirmó que este comando funciona en la cámara:
                // top -bn1 | grep '^CPU' | tr -d '%' | awk '{print $2 + $4}'

                // En Kotlin escapamos el $ con \$ para que no piense que es una variable.
                "top -bn1 | grep '^CPU' | tr -d '%' | awk '{print \$2 + \$4}'"
            } else {
                // ESTRATEGIA SERVIDOR (Linux Estándar - OrangePi/Ubuntu/Debian)
                // Salida típica: %Cpu(s): 10.0 us,  2.0 sy...
                // Usamos sed para limpiar la línea antes de pasarla a awk, es más seguro que grep a veces.
                // 1. top -bn1: Ejecuta top una vez.
                // 2. grep "Cpu(s)": Filtra la línea de CPU.
                // 3. sed "s/.*, *\([0-9.]*\)%* id.*/\1/": TRUCO -> Extraemos el IDLE (inactividad) y restamos de 100.
                //    (Es más universal porque el orden de us/sys puede cambiar, pero idle suele estar etiquetado).

                // OPCIÓN SIMPLIFICADA QUE FALLA MENOS (Suma US + SY):
                // Kotlin envía: awk '{print $2 + $4}'
                "top -bn1 | grep -i 'Cpu(s)' | awk '{print \$2 + \$4}'"
            }

            val cpuVal = try {
                val output = client.execOneCommand(cpuCmd)
                // Limpiamos la salida por si se cuela el prompt o saltos de línea extraños
                val cleanOutput = output.lines().firstOrNull { it.any { c -> c.isDigit() } } ?: "0.0"
                cleanOutput.trim().toDoubleOrNull() ?: 0.0
            } catch (e: Exception) {
                0.0
            }

            // --- 2. COMANDO DE RAM (Sin cambios, parecía funcionar) ---
            val ramCmd = if (server.type == DeviceType.CAMERA) {
                // BusyBox suele necesitar free simple
                "free | awk 'NR==2{printf \"%.2f\", \$3*100/\$2 }'"
            } else {
                // Linux estándar soporta -m
                "free -m | awk 'NR==2{printf \"%.2f\", \$3*100/\$2 }'"
            }

            val ramVal = try {
                val output = client.execOneCommand(ramCmd)
                output.trim().toDoubleOrNull() ?: 0.0
            } catch (e: Exception) { 0.0 }

            // ... (Resto del código igual: Disco, Temperatura, Puertos) ...

            // --- CÓDIGO RESTANTE RESUMIDO ---
            val diskVal = try {
                val cmd = "df -P / | awk 'NR==2{print \$5}' | tr -d '%'"
                client.execOneCommand(cmd).toDoubleOrNull() ?: 0.0
            } catch (e: Exception) { 0.0 }

            val tempVal = try {
                val cmd = "cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null || cat /sys/class/hwmon/hwmon0/temp1_input 2>/dev/null"
                val raw = client.execOneCommand(cmd)
                (raw.toDoubleOrNull() ?: 0.0) / 1000.0
            } catch (e: Exception) { 0.0 }

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
    // Helper para parsear la salida de netstat
    private fun parsePorts(netstatOutput: String): List<PortInfo> {
        val list = mutableListOf<PortInfo>()
        val lines = netstatOutput.lines()

        for (line in lines) {
            if (line.isBlank()) continue
            // Dividimos por espacios múltiples
            val parts = line.trim().split("\\s+".toRegex())

            // netstat output típico:
            // tcp 0 0 0.0.0.0:80 0.0.0.0:* LISTEN 1524/httpd
            if (parts.size >= 4) {
                try {
                    val protocol = parts[0] // tcp / udp
                    val localAddress = parts[3] // 0.0.0.0:80 o :::22

                    // Extraer puerto (último número tras los dos puntos)
                    val portString = localAddress.substringAfterLast(":")
                    val port = portString.toIntOrNull()

                    // Extraer nombre del proceso (columna PID/Program name, a veces es la última)
                    // Buscamos la columna que contiene "/"
                    val pidProgram = parts.find { it.contains("/") } ?: "?"
                    val processName = pidProgram.substringAfter("/") // Nos quedamos solo con el nombre (httpd)

                    if (port != null) {
                        list.add(PortInfo(port, protocol, processName))
                    }
                } catch (e: Exception) {
                    // Ignoramos líneas mal formadas
                }
            }
        }
        // Ordenamos por número de puerto y eliminamos duplicados (tcp/tcp6 a veces duplica)
        return list.distinctBy { it.port }.sortedBy { it.port }
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

    override suspend fun fetchProcesses(server: Server): Result<List<ProcessInfo>> = withContext(Dispatchers.IO) {
        val client = SSHClient()
        try {
            client.connectAndAuthenticate(server)

            val processes = if (server.type == DeviceType.CAMERA) {
                // ESTRATEGIA CÁMARA (BusyBox Top)
                // Usamos top en modo batch (-b) una vez (-n 1)
                val cmd = "top -b -n 1"
                val output = client.execOneCommand(cmd)
                parseBusyBoxTop(output)
            } else {
                // ESTRATEGIA SERVIDOR (Linux Standard PS)
                // -e: todos, -o: formato custom, --sort: ordenar por cpu descendente
                // Formato: PID USER %CPU %MEM COMMAND
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

    private fun parseLinuxPs(output: String): List<ProcessInfo> {
        val list = mutableListOf<ProcessInfo>()
        val lines = output.lines().drop(1) // Saltamos cabecera: PID USER %CPU %MEM COMMAND

        for (line in lines) {
            if (line.isBlank()) continue
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size >= 5) {
                try {
                    list.add(ProcessInfo(
                        pid = parts[0],
                        user = parts[1],
                        cpuUsage = parts[2].toDoubleOrNull() ?: 0.0,
                        memUsage = parts[3].toDoubleOrNull() ?: 0.0,
                        command = parts.subList(4, parts.size).joinToString(" ")
                    ))
                } catch (e: Exception) { /* ignore line */ }
            }
        }
        return list
    }

    private fun parseBusyBoxTop(output: String): List<ProcessInfo> {
        // Output típico Yi Camera:
        // PID  PPID USER     STAT   VSZ %VSZ CPU %CPU COMMAND
        // 1416    1 root     S     36388 59.5   0 56.0 ./rmm
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
            // Necesitamos al menos hasta el comando.
            // Indices Yi: PID(0) PPID(1) USER(2) STAT(3) VSZ(4) %VSZ(5) CPU(6) %CPU(7) COMMAND(8)
            if (parts.size >= 9) {
                try {
                    list.add(ProcessInfo(
                        pid = parts[0],
                        user = parts[2], // User está en el índice 2
                        cpuUsage = parts[7].replace("%","").toDoubleOrNull() ?: 0.0, // %CPU
                        memUsage = parts[5].replace("%","").toDoubleOrNull() ?: 0.0, // %VSZ (Memoria virtual aprox)
                        command = parts.subList(8, parts.size).joinToString(" ")
                    ))
                } catch (e: Exception) { /* ignore */ }
            }
        }
        return list
    }

}