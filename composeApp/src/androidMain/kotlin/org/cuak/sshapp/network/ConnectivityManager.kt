package org.cuak.sshapp.network

import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ConnectivityManager {
    actual suspend fun isReachable(host: String, timeout: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            InetAddress.getByName(host).isReachable(timeout)
        } catch (e: Exception) {
            false
        }
    }
}