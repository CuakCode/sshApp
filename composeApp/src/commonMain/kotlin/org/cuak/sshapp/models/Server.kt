package org.cuak.sshapp.models

data class Server(
    val id: Long = 0,
    val name: String,
    val ip: String,
    val port: Int = 22,
    val username: String,
    val status: ServerStatus = ServerStatus.UNKNOWN,
    val iconName: String = "dns"
)

enum class ServerStatus { ONLINE, OFFLINE, UNKNOWN }

