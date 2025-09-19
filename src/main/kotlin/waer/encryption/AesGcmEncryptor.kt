package waer.encryption

import waer.interfaces.Encryptor
import waer.interfaces.EncryptedData
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-GCM implementation of the Encryptor interface
 */
class AesGcmEncryptor(private val key: ByteArray) : Encryptor {
    private val secureRandom = SecureRandom()

    init {
        require(key.size == 16 || key.size == 24 || key.size == 32) {
            "AES key must be 128/192/256 bits"
        }
    }

    override fun encrypt(plaintext: ByteArray): EncryptedData {
        val nonce = ByteArray(12)
        secureRandom.nextBytes(nonce)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val spec = GCMParameterSpec(128, nonce)

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)
        val ciphertextAndTag = cipher.doFinal(plaintext)

        val tag = ciphertextAndTag.copyOfRange(ciphertextAndTag.size - 16, ciphertextAndTag.size)
        val ciphertext = ciphertextAndTag.copyOfRange(0, ciphertextAndTag.size - 16)

        return EncryptedData(
            version = 1,
            nonce = nonce,
            ciphertext = ciphertext,
            tag = tag,
            plaintextLength = plaintext.size
        )
    }

    override fun decrypt(encryptedData: EncryptedData): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(key, "AES")
        val spec = GCMParameterSpec(128, encryptedData.nonce)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)

        val ciphertextAndTag = ByteArray(encryptedData.ciphertext.size + encryptedData.tag.size)
        System.arraycopy(encryptedData.ciphertext, 0, ciphertextAndTag, 0, encryptedData.ciphertext.size)
        System.arraycopy(encryptedData.tag, 0, ciphertextAndTag, encryptedData.ciphertext.size, encryptedData.tag.size)

        return cipher.doFinal(ciphertextAndTag)
    }
}
