package org.cuak.sshapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform