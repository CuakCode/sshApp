package org.cuak.sshapp.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException

actual class ConnectivityManager actual constructor() {
    actual suspend fun isReachable(host: String, timeout: Int): Boolean = withContext(Dispatchers.IO) {
        println("🔍 [Ping] Iniciando comprobación para: $host")
        println("El tiempo de ping es $timeout")
        try {
            // 1. Intento de resolución de DNS
            val address = InetAddress.getByName(host)
            println("✅ [DNS] Resuelto con éxito: $host -> ${address.hostAddress}")

            // 2. Intento de Ping (ICMP / TCP Port 7)
            val reachable = address.isReachable(timeout)

            if (reachable) {
                println("🌐 [Resultado] El host $host [${address.hostAddress}] está ONLINE.")
            } else {
                println("⚠️ [Resultado] El host $host [${address.hostAddress}] NO responde (Timeout de ${timeout}ms).")
                println("   Nota: En Windows/Linux sin privilegios, isReachable intenta el puerto 7 (Echo), que suele estar cerrado.")
            }

            reachable
        } catch (e: UnknownHostException) {
            println("❌ [DNS Error] No se pudo resolver el dominio '$host'. Revisa la conexión o el nombre.")
            false
        } catch (e: Exception) {
            println("❌ [Error Inesperado] Fallo al comprobar $host: ${e.message}")
            e.printStackTrace()
            false
        }
    }
}