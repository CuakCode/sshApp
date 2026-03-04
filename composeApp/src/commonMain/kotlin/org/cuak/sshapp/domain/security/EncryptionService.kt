package org.cuak.sshapp.domain.security


interface EncryptionService {
    fun encrypt(rawData: String?): String?
    fun decrypt(encryptedData: String?): String?
}


expect class PlatformEncryptionService() : EncryptionService {
    override fun encrypt(rawData: String?): String?
    override fun decrypt(encryptedData: String?): String?
}