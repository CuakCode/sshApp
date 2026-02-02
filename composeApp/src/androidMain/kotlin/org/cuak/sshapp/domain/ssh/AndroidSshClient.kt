package org.cuak.sshapp.domain.ssh

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.SecurityUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
class AndroidSshClient : AbstractSshjClient() {

    init {
        // ESTO ES LO ÚNICO ESPECÍFICO DE ANDROID
        try {
            Security.removeProvider("BC")
            Security.addProvider(BouncyCastleProvider())
            SecurityUtils.setSecurityProvider("BC")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}