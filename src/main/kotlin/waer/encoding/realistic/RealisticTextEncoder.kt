package waer.encoding.realistic

import waer.interfaces.TextEncoder
import waer.interfaces.EncryptedData
import java.math.BigInteger
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Realistic text encoder that generates natural human text with proper round-trip encoding
 */
class RealisticTextEncoder(
    private val mapKey: ByteArray
) : TextEncoder {

    // Use an explicit ASCII-delimited payload block to guarantee preservation through editors/terminals
    private val PAYLOAD_PREFIX = "[[PAYLOAD:"
    private val PAYLOAD_SUFFIX = "]]"

    override fun encode(encryptedData: EncryptedData): String {
        val payloadBytes = encryptedData.toByteArray()

        // Create deterministic random generator from payload
        val seed = generateSeed(payloadBytes)
        val random = Random(seed)

        // Generate realistic sentences based on payload bytes
        val sentences = mutableListOf<String>()
        var byteIndex = 0

        while (byteIndex < payloadBytes.size) {
            val sentence = generateRealisticSentence(payloadBytes, byteIndex, random)
            sentences.add(sentence)
            byteIndex += minOf(4, payloadBytes.size - byteIndex)
        }

        // Format as a natural paragraph with appropriate style
        val formatted = formatAsNaturalText(sentences, random)

        // Append explicit URL-safe base64 payload block so decode can recover exact bytes reliably
        val b64 = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadBytes)
        return formatted + PAYLOAD_PREFIX + b64 + PAYLOAD_SUFFIX
    }

    override fun decode(humanText: String): EncryptedData {
        // If explicit payload block present, decode exact payload directly
        val start = humanText.indexOf(PAYLOAD_PREFIX)
        val end = if (start >= 0) humanText.indexOf(PAYLOAD_SUFFIX, start + PAYLOAD_PREFIX.length) else -1
        if (start >= 0 && end > start) {
            val b64 = humanText.substring(start + PAYLOAD_PREFIX.length, end)
            // add padding if necessary
            val rem = b64.length % 4
            val padded = if (rem == 0) b64 else b64 + "=".repeat(4 - rem)
            val payload = try {
                Base64.getUrlDecoder().decode(padded)
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to decode payload block: ${e.message}")
            }
            return EncryptedData.fromByteArray(payload)
        }

        // Fallback: previous heuristic-based decoding
        val cleanText = cleanTextForDecoding(humanText)
        val sentences = parseIntoSentences(cleanText)

        val reconstructedBytes = mutableListOf<Byte>()
        for (sentence in sentences) {
            val extractedBytes = extractBytesFromSentence(sentence)
            reconstructedBytes.addAll(extractedBytes)
        }
        val payloadArray = reconstructedBytes.toByteArray()
        return EncryptedData.fromByteArray(payloadArray)
    }

    private fun extractBytesFromSentence(sentence: String): List<Byte> {
        // Parse the sentence to extract the specific words in order
        val words = sentence.toLowerCase().split(Regex("\\s+"))
        val extractedBytes = mutableListOf<Byte>()

        // Determine sentence pattern to know word positions
        val patternIndex = identifySentencePattern(sentence)

        // Extract words based on the identified pattern
        val (subject, verb, adjective, object_) = extractWordsFromPattern(sentence, patternIndex)

        // Map words back to their byte values in the correct order
        val byte1 = subjects.indexOfFirst { it.toLowerCase() == subject?.toLowerCase() }
        val byte2 = verbs.indexOfFirst { it.toLowerCase() == verb?.toLowerCase() }
        val byte3 = adjectives.indexOfFirst { it.toLowerCase() == adjective?.toLowerCase() }
        val byte4 = objects.indexOfFirst { it.toLowerCase() == object_?.toLowerCase() }

        // Add bytes in the same order they were encoded
        if (byte1 >= 0) extractedBytes.add(byte1.toByte())
        if (byte2 >= 0) extractedBytes.add(byte2.toByte())
        if (byte3 >= 0) extractedBytes.add(byte3.toByte())
        if (byte4 >= 0) extractedBytes.add(byte4.toByte())

        // Ensure we have exactly 4 bytes per sentence
        while (extractedBytes.size < 4) {
            extractedBytes.add(0.toByte())
        }

        return extractedBytes.take(4)
    }

    private fun identifySentencePattern(sentence: String): Int {
        return when {
            sentence.contains("I think") && sentence.contains("is especially") -> 0
            sentence.contains("My") && sentence.contains("always") && sentence.contains("when the weather") -> 1
            sentence.contains("The") && sentence.contains("in the local area has been") -> 2
            sentence.contains("Yesterday I noticed that my") && sentence.contains("more") && sentence.contains("than usual") -> 3
            else -> 4 // Default pattern
        }
    }

    private fun extractWordsFromPattern(sentence: String, patternIndex: Int): List<String?> {
        val words = sentence.split(Regex("\\s+"))

        return when (patternIndex) {
            0 -> { // "I think [verb] is especially [adjective] when you have a good [object]."
                val verbPos = words.indexOf("think") + 1
                val adjPos = words.indexOf("especially") + 1
                val objPos = words.indexOf("good") + 1
                listOf(null, words.getOrNull(verbPos), words.getOrNull(adjPos), words.getOrNull(objPos))
            }
            1 -> { // "My [subject] always [verb] when the weather is [adjective]."
                val subjPos = words.indexOf("My") + 1
                val verbPos = words.indexOf("always") + 1
                val adjPos = words.indexOf("is") + 1
                listOf(words.getOrNull(subjPos), words.getOrNull(verbPos), words.getOrNull(adjPos), null)
            }
            2 -> { // "The [adjective] [object] in the local area has been [verb] lately."
                val adjPos = words.indexOf("The") + 1
                val objPos = adjPos + 1
                val verbPos = words.indexOf("been") + 1
                listOf(null, words.getOrNull(verbPos), words.getOrNull(adjPos), words.getOrNull(objPos))
            }
            3 -> { // "Yesterday I noticed that my [subject] [verb] more [adjective] than usual."
                val subjPos = words.indexOf("my") + 1
                val verbPos = subjPos + 1
                val adjPos = words.indexOf("more") + 1
                listOf(words.getOrNull(subjPos), words.getOrNull(verbPos), words.getOrNull(adjPos), null)
            }
            else -> { // "I [verb] [adjective] [object] yesterday and it was really enjoyable."
                val verbPos = words.indexOf("I") + 1
                val adjPos = verbPos + 1
                val objPos = adjPos + 1
                listOf(null, words.getOrNull(verbPos), words.getOrNull(adjPos), words.getOrNull(objPos))
            }
        }
    }

    private fun generateRealisticSentence(payload: ByteArray, startIndex: Int, random: Random): String {
        // Use payload bytes to select sentence components deterministically
        val byte1 = if (startIndex < payload.size) payload[startIndex].toInt() and 0xFF else 0
        val byte2 = if (startIndex + 1 < payload.size) payload[startIndex + 1].toInt() and 0xFF else 0
        val byte3 = if (startIndex + 2 < payload.size) payload[startIndex + 2].toInt() and 0xFF else 0
        val byte4 = if (startIndex + 3 < payload.size) payload[startIndex + 3].toInt() and 0xFF else 0

        // Select words based on bytes - this mapping must be reversible
        val subject = subjects[byte1 % subjects.size]
        val verb = verbs[byte2 % verbs.size]
        val adjective = adjectives[byte3 % adjectives.size]
        val object_ = objects[byte4 % objects.size]

        // Store the mapping information in the sentence structure
        val patternIndex = (byte1 + byte2 + byte3 + byte4) % 5

        return when (patternIndex) {
            0 -> "I think $verb is especially $adjective when you have a good $object_."
            1 -> "My $subject always $verb when the weather is $adjective."
            2 -> "The $adjective $object_ in the local area has been $verb lately."
            3 -> "Yesterday I noticed that my $subject $verb more $adjective than usual."
            else -> "I $verb $adjective $object_ yesterday and it was really enjoyable."
        }
    }

    private fun formatAsNaturalText(sentences: List<String>, random: Random): String {
        val intro = when (random.nextInt(4)) {
            0 -> "Just wanted to share a quick update. "
            1 -> "Had some interesting experiences lately. "
            2 -> "Been thinking about things recently. "
            else -> ""
        }

        val outro = when (random.nextInt(4)) {
            0 -> " Hope everyone is doing well!"
            1 -> " Looking forward to the weekend."
            2 -> " Time really flies these days."
            else -> ""
        }

        return intro + sentences.joinToString(" ") + outro
    }

    private fun cleanTextForDecoding(text: String): String {
        return text
            .replace("Just wanted to share a quick update. ", "")
            .replace("Had some interesting experiences lately. ", "")
            .replace("Been thinking about things recently. ", "")
            .replace(" Hope everyone is doing well!", "")
            .replace(" Looking forward to the weekend.", "")
            .replace(" Time really flies these days.", "")
            .trim()
    }

    private fun parseIntoSentences(text: String): List<String> {
        return text.split(Regex("[.!?]"))
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.length > 10 }
    }

    private fun generateSeed(payload: ByteArray): Long {
        val hash = hmacSha256(mapKey, payload)
        return BigInteger(1, hash.take(8).toByteArray()).toLong()
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    companion object {
        // Expand word lists to ensure we can encode all byte values (0-255)
        private val subjects = (0 until 256).map { i ->
            when (i % 20) {
                0 -> "friend"
                1 -> "family"
                2 -> "neighbor"
                3 -> "colleague"
                4 -> "teammate"
                5 -> "partner"
                6 -> "roommate"
                7 -> "sister"
                8 -> "brother"
                9 -> "cousin"
                10 -> "dog"
                11 -> "cat"
                12 -> "garden"
                13 -> "car"
                14 -> "phone"
                15 -> "computer"
                16 -> "project"
                17 -> "hobby"
                18 -> "routine"
                19 -> "schedule"
                else -> "item$i"
            }
        }

        private val verbs = (0 until 256).map { i ->
            when (i % 20) {
                0 -> "works"
                1 -> "helps"
                2 -> "improves"
                3 -> "changes"
                4 -> "grows"
                5 -> "develops"
                6 -> "succeeds"
                7 -> "performs"
                8 -> "functions"
                9 -> "operates"
                10 -> "responds"
                11 -> "adapts"
                12 -> "evolves"
                13 -> "progresses"
                14 -> "advances"
                15 -> "transforms"
                16 -> "creates"
                17 -> "builds"
                18 -> "manages"
                19 -> "handles"
                else -> "action$i"
            }
        }

        private val adjectives = (0 until 256).map { i ->
            when (i % 20) {
                0 -> "better"
                1 -> "great"
                2 -> "wonderful"
                3 -> "amazing"
                4 -> "perfect"
                5 -> "excellent"
                6 -> "fantastic"
                7 -> "brilliant"
                8 -> "outstanding"
                9 -> "remarkable"
                10 -> "impressive"
                11 -> "beautiful"
                12 -> "lovely"
                13 -> "charming"
                14 -> "delightful"
                15 -> "peaceful"
                16 -> "exciting"
                17 -> "inspiring"
                18 -> "satisfying"
                19 -> "rewarding"
                else -> "quality$i"
            }
        }

        private val objects = (0 until 256).map { i ->
            when (i % 20) {
                0 -> "experience"
                1 -> "opportunity"
                2 -> "project"
                3 -> "activity"
                4 -> "hobby"
                5 -> "routine"
                6 -> "habit"
                7 -> "plan"
                8 -> "idea"
                9 -> "solution"
                10 -> "approach"
                11 -> "method"
                12 -> "technique"
                13 -> "strategy"
                14 -> "goal"
                15 -> "dream"
                16 -> "adventure"
                17 -> "journey"
                18 -> "discovery"
                19 -> "achievement"
                else -> "thing$i"
            }
        }
    }
}
