package waer.interfaces

/**
 * Step 2: Encrypt plaintext into binary encrypted data
 */
interface Encryptor {
    fun encrypt(plaintext: ByteArray): EncryptedData
    fun decrypt(encryptedData: EncryptedData): ByteArray
}

/**
 * Step 3: Convert encrypted binary data into human-readable text
 */
interface TextEncoder {
    fun encode(encryptedData: EncryptedData): String
    fun decode(humanText: String): EncryptedData
}

/**
 * Represents encrypted binary data with metadata
 */
data class EncryptedData(
    val version: Int,
    val nonce: ByteArray,
    val ciphertext: ByteArray,
    val tag: ByteArray,
    val plaintextLength: Int
) {
    fun toByteArray(): ByteArray {
        val result = ByteArray(1 + 12 + 16 + 4 + ciphertext.size)
        var offset = 0

        // version (1 byte)
        result[offset++] = version.toByte()

        // nonce (12 bytes)
        System.arraycopy(nonce, 0, result, offset, 12)
        offset += 12

        // tag (16 bytes)
        System.arraycopy(tag, 0, result, offset, 16)
        offset += 16

        // plaintext length (4 bytes)
        result[offset++] = ((plaintextLength ushr 24) and 0xFF).toByte()
        result[offset++] = ((plaintextLength ushr 16) and 0xFF).toByte()
        result[offset++] = ((plaintextLength ushr 8) and 0xFF).toByte()
        result[offset++] = (plaintextLength and 0xFF).toByte()

        // ciphertext
        System.arraycopy(ciphertext, 0, result, offset, ciphertext.size)

        return result
    }

    companion object {
        fun fromByteArray(data: ByteArray): EncryptedData {
            require(data.size >= 33) { "Invalid encrypted data: too short" }

            var offset = 0

            // version
            val version = data[offset++].toInt()

            // nonce
            val nonce = data.copyOfRange(offset, offset + 12)
            offset += 12

            // tag
            val tag = data.copyOfRange(offset, offset + 16)
            offset += 16

            // plaintext length
            val plaintextLength = ((data[offset].toInt() and 0xFF) shl 24) or
                                ((data[offset + 1].toInt() and 0xFF) shl 16) or
                                ((data[offset + 2].toInt() and 0xFF) shl 8) or
                                (data[offset + 3].toInt() and 0xFF)
            offset += 4

            // ciphertext
            val ciphertext = if (offset < data.size) {
                data.copyOfRange(offset, data.size)
            } else {
                ByteArray(0)
            }

            return EncryptedData(version, nonce, ciphertext, tag, plaintextLength)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as EncryptedData
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

/**
 * Main orchestrator that combines all steps
 */
interface TransparentTextProcessor {
    fun encodeToHumanText(plaintext: ByteArray): String
    fun decodeFromHumanText(humanText: String): ByteArray
}
