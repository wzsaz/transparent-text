package waer

import java.nio.charset.StandardCharsets

fun main() {
    // deterministic demo keys (do NOT use fixed keys in production)
    val aeadKey = ByteArray(32) { i -> (i and 0xFF).toByte() }
    val mapKey = ByteArray(32) { i -> ((i * 3) and 0xFF).toByte() }

    val crypto = Crypto(aeadKey)

    val plaintext = "Secret message".toByteArray(StandardCharsets.UTF_8)

    // compute expected payloadWithoutTemplate length: version(1) + nonce(12) + tag(16) + plaintextLength(4) + ciphertext(==plaintext.size)
    val expectedPayloadLen = 1 + 12 + 16 + 4 + plaintext.size

    // build a template that can represent any byte sequence of expectedPayloadLen bytes
    // use 256-word buckets for each byte (radix 256)
    val bucketWords = (0 until 256).map { i -> "w${i}" }
    val buckets = List(expectedPayloadLen) { bucketWords }

    // patternParts: empty prefix, then a space between each slot, then empty suffix
    val patternParts = MutableList(expectedPayloadLen + 1) { " " }
    patternParts[0] = ""
    patternParts[expectedPayloadLen] = ""

    val t = Template(
        id = 0,
        patternParts = patternParts,
        buckets = buckets
    )

    val templates = listOf(t)
    val codec = Codec(templates = templates, mapKey = mapKey, crypto = crypto)

    val sentence = codec.encode(plaintext)
    println("Sentence: $sentence")

    val recovered = codec.decode(sentence)
    println("Recovered: ${String(recovered, StandardCharsets.UTF_8)}")
}
