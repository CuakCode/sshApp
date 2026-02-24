package org.cuak.sshapp.domain.security

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

actual class PlatformEncryptionService actual constructor() : EncryptionService {

    private val keyBytes = byteArrayOf(
        0x2b, 0x7e, 0x15, 0x16, 0x28, 0xae.toByte(), 0xd2.toByte(), 0xa6.toByte(),
        0xab.toByte(), 0xf7.toByte(), 0x15, 0x88.toByte(), 0x09, 0xcf.toByte(), 0x4f, 0x3c
    )
    private val secretKey = SecretKeySpec(keyBytes, "AES")
    private val TRANSFORMATION = "AES"

    actual override fun encrypt(rawData: String?): String? {
        if (rawData == null) return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedBytes = cipher.doFinal(rawData.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(encryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual override fun decrypt(encryptedData: String?): String? {
        if (encryptedData == null) return null
        return try {
            val decodedBytes = Base64.getDecoder().decode(encryptedData)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            String(cipher.doFinal(decodedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            encryptedData
        }
    }
}