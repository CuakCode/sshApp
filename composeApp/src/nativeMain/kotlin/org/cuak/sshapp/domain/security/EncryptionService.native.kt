package org.cuak.sshapp.domain.security

actual class PlatformEncryptionService actual constructor() :
    org.cuak.sshapp.domain.security.EncryptionService {
    actual override fun encrypt(rawData: String?): String? {
        TODO("Not yet implemented")
    }

    actual override fun decrypt(encryptedData: String?): String? {
        TODO("Not yet implemented")
    }
}