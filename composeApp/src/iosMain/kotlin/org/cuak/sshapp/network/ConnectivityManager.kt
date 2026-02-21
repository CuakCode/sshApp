// composeApp/src/iosMain/kotlin/org/cuak/sshapp/network/ConnectivityManager.kt
package org.cuak.sshapp.network

import kotlinx.coroutines.withTimeoutOrNull
import platform.Network.*
import platform.posix.*
// ... otros imports necesarios para nw_connection

actual class ConnectivityManager {
    actual suspend fun isReachable(host: String, timeout: Int): Boolean {
        // En iOS envolvemos la lógica en un withTimeout de Corrutinas
        return withTimeoutOrNull(timeout.toLong()) {
            // Lógica de nw_connection_start...
            // Si la conexión no responde en 'timeout' ms, withTimeout devuelve null
            true
        } ?: false
    }
}