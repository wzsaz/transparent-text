package waer

import waer.encoding.realistic.RealisticTextEncoder
import waer.encryption.AesGcmEncryptor
import java.nio.charset.StandardCharsets
import kotlin.experimental.and

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

fun main() {
    println("=== Realistic Text Demo ===\n")

    try {
        // Setup keys (do NOT use fixed keys in production)
        val aeadKey = ByteArray(32) { i -> (i and 0xFF).toByte() }
        val mapKey = ByteArray(32) { i -> ((i * 3) and 0xFF).toByte() }

        val plaintext = "Secret message".toByteArray(StandardCharsets.UTF_8)
        println("Original plaintext: ${String(plaintext, StandardCharsets.UTF_8)}")

        // Test just the realistic text generation first
        val encryptor = AesGcmEncryptor(aeadKey)
        val encryptedData = encryptor.encrypt(plaintext)

        val textEncoder = RealisticTextEncoder(mapKey)
        val humanText = textEncoder.encode(encryptedData)

        println("\nRealistic human text:")
        println("\"$humanText\"")

        println("\n✓ Realistic text generation successful!")
        println("✓ This looks like real human conversation, not random words!")

        val originalData = textEncoder.decode(humanText)

        // Diagnostic: compare byte arrays before attempting decrypt
        val origBytes = encryptedData.toByteArray()
        val decodedBytes = originalData.toByteArray()

        println("\nEncrypted payload length: original=${origBytes.size} decoded=${decodedBytes.size}")
        val equal = origBytes.contentEquals(decodedBytes)
        println("Payload exact match: $equal")
        if (!equal) {
            println("Original (hex): ${origBytes.toHex()}")
            println("Decoded  (hex): ${decodedBytes.toHex()}")
            // Print first differing index
            val min = minOf(origBytes.size, decodedBytes.size)
            for (i in 0 until min) {
                if (origBytes[i] != decodedBytes[i]) {
                    println("First difference at byte $i: orig=${"%02x".format(origBytes[i])} dec=${"%02x".format(decodedBytes[i])}")
                    break
                }
            }
            println("Aborting decrypt to avoid AEAD tag mismatch. Investigate encoding/decoding logic.")
            return
        }

        val decryptedData = encryptor.decrypt(originalData)
        println("\nDecrypted plaintext: ${String(decryptedData, StandardCharsets.UTF_8)}")
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}
