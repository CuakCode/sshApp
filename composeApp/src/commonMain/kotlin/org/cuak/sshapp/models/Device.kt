package org.cuak.sshapp.models

enum class ServerStatus { ONLINE, OFFLINE, UNKNOWN }


sealed interface Device {
    val id: Long
    val name: String
    val ip: String
    val port: Int
    val username: String
    val password: String?
    val sshKeyPath: String?
    val status: ServerStatus
    val iconName: String
}


data class Server(
    override val id: Long = 0,
    override val name: String,
    override val ip: String,
    override val port: Int = 22,
    override val username: String,
    override val password: String? = null,
    override val sshKeyPath: String? = null,
    override val status: ServerStatus = ServerStatus.UNKNOWN,
    override val iconName: String = "dns"
) : Device


data class Camera(
    override val id: Long = 0,
    override val name: String,
    override val ip: String,
    override val port: Int = 22, 
    override val username: String = "root",
    override val password: String? = null,
    override val sshKeyPath: String? = null,
    override val status: ServerStatus = ServerStatus.UNKNOWN,
    override val iconName: String = "videocam",

    
    val cameraProtocol: String = "RTSP",
    val cameraPort: Int = 8554,
    val cameraStream: String = "ch0_0.h264"
) : Device {

    
    val rtspUrl: String
        get() = "$cameraProtocol://$ip:$cameraPort/$cameraStream"
}