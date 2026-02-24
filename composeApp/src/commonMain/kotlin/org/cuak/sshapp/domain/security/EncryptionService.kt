package org.cuak.sshapp.domain.security

// 1. El contrato
interface EncryptionService {
    fun encrypt(rawData: String?): String?
    fun decrypt(encryptedData: String?): String?
}

// 2. La clase "promesa" que instanciará Koin (DEBE llevar los override)
expect class PlatformEncryptionService() : EncryptionService {
    override fun encrypt(rawData: String?): String?
    override fun decrypt(encryptedData: String?): String?
}