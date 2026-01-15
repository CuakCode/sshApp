package org.cuak.sshapp.network

import java.net.InetSocketAddress
import java.net.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

actual class ConnectivityManager actual constructor() {
    actual suspend fun isReachable(host: String, timeout: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            // getByName resuelve el DNS automáticamente
            val address = InetAddress.getByName(host)
            address.isReachable(timeout)
        } catch (e: Exception) {
            false // Error en DNS o host inalcanzable
        }
    }
}