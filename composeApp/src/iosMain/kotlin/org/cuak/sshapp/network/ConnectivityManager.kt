// iosMain
package org.cuak.sshapp.network

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import platform.Network.*
import platform.darwin.dispatch_get_main_queue

actual class ConnectivityManager actual constructor() {

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun isReachable(host: String, timeout: Int): Boolean {
        val result = CompletableDeferred<Boolean>()

        // El framework de Apple resuelve el DNS automáticamente al crear el endpoint
        val endpoint = nw_endpoint_create_host(host, "0")
        val parameters = nw_parameters_create_secure_tcp(
            NW_PARAMETERS_DEFAULT_CONFIGURATION,
            NW_PARAMETERS_DEFAULT_CONFIGURATION
        )

        val connection = nw_connection_create(endpoint, parameters)

        nw_connection_set_state_changed_handler(connection) { state, _ ->
            when (state) {
                // Si llega a ready o waiting, el host existe y es alcanzable
                nw_connection_state_ready, nw_connection_state_waiting -> {
                    if (result.isActive) result.complete(true)
                }
                nw_connection_state_failed -> {
                    if (result.isActive) result.complete(false)
                }
                else -> {}
            }
        }

        nw_connection_set_queue(connection, dispatch_get_main_queue())
        nw_connection_start(connection)

        return try {
            withTimeoutOrNull(timeout.toLong()) {
                result.await()
            } ?: false
        } catch (e: Exception) {
            false
        } finally {
            nw_connection_cancel(connection)
        }
    }
}