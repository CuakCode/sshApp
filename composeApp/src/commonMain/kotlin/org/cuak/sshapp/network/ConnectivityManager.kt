package org.cuak.sshapp.network

expect class ConnectivityManager() {
    suspend fun isReachable(host: String, timeout: Int): Boolean //
}