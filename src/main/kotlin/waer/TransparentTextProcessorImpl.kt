package waer

import waer.interfaces.Encryptor
import waer.interfaces.TextEncoder
import waer.interfaces.TransparentTextProcessor

/**
 * Main processor that orchestrates the 5-step workflow:
 * 1. Take raw plaintext
 * 2. Encrypt the text
 * 3. Convert encrypted text into reversible plaintext representation (human-readable)
 * 4. Convert "plain-encrypted" text back into encrypted form
 * 5. Decrypt
 */
class TransparentTextProcessorImpl(
    private val encryptor: Encryptor,
    private val textEncoder: TextEncoder
) : TransparentTextProcessor {

    /**
     * Steps 1-3: plaintext -> encrypted -> human-readable text
     */
    override fun encodeToHumanText(plaintext: ByteArray): String {
        // Step 2: Encrypt plaintext
        val encryptedData = encryptor.encrypt(plaintext)

        // Step 3: Convert encrypted data to human-readable text
        return textEncoder.encode(encryptedData)
    }

    /**
     * Steps 4-5: human-readable text -> encrypted -> plaintext
     */
    override fun decodeFromHumanText(humanText: String): ByteArray {
        // Step 4: Convert human-readable text back to encrypted data
        val encryptedData = textEncoder.decode(humanText)

        // Step 5: Decrypt to recover original plaintext
        return encryptor.decrypt(encryptedData)
    }
}
