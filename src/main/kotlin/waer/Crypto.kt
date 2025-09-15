package waer

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AeadPayload(
    val version: Int,
    val nonce: ByteArray,
    val ciphertext: ByteArray,
    val tag: ByteArray,
    val plaintextLength: Int
) {
    // keep default equals/hashCode behavior for arrays by delegating to contentEquals/contentHashCode if needed
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as AeadPayload
        if (version != other.version) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!tag.contentEquals(other.tag)) return false
        if (plaintextLength != other.plaintextLength) return false
        return true
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + tag.contentHashCode()
        result = 31 * result + plaintextLength
        return result
    }
}

class Crypto(private val aeadKey: ByteArray) {
    private val secureRandom = SecureRandom()

    fun encrypt(plaintext: ByteArray, associatedData: ByteArray? = null): AeadPayload {
        require(aeadKey.size == 16 || aeadKey.size == 24 || aeadKey.size == 32) { "AES key must be 128/192/256 bits" }
        val nonce = ByteArray(12)
        secureRandom.nextBytes(nonce)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(aeadKey, "AES")
        val spec = GCMParameterSpec(128, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)
        if (associatedData != null) cipher.updateAAD(associatedData)
        val ciphertextAndTag = cipher.doFinal(plaintext)
        val tag = ciphertextAndTag.copyOfRange(ciphertextAndTag.size - 16, ciphertextAndTag.size)
        val ciphertext = ciphertextAndTag.copyOfRange(0, ciphertextAndTag.size - 16)
        return AeadPayload(version = 1, nonce = nonce, ciphertext = ciphertext, tag = tag, plaintextLength = plaintext.size)
    }

    fun decrypt(payload: AeadPayload, associatedData: ByteArray? = null): ByteArray {
        require(aeadKey.size == 16 || aeadKey.size == 24 || aeadKey.size == 32) { "AES key must be 128/192/256 bits" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(aeadKey, "AES")
        val spec = GCMParameterSpec(128, payload.nonce)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)
        if (associatedData != null) cipher.updateAAD(associatedData)
        val ciphertextAndTag = ByteArray(payload.ciphertext.size + payload.tag.size)
        System.arraycopy(payload.ciphertext, 0, ciphertextAndTag, 0, payload.ciphertext.size)
        System.arraycopy(payload.tag, 0, ciphertextAndTag, payload.ciphertext.size, payload.tag.size)
        return cipher.doFinal(ciphertextAndTag)
    }
}
