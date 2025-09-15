package waer

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class Codec(
    private val templates: List<Template>,
    private val mapKey: ByteArray,
    private val crypto: Crypto
) {
    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun bucketProduct(t: Template): BigInteger {
        var p = BigInteger.ONE
        for (r in t.buckets.map { it.size }) {
            p = p.multiply(BigInteger.valueOf(r.toLong()))
        }
        return p
    }

    private fun rotationOrder(seed: ByteArray, n: Int): List<Int> {
        val start = BigInteger(1, seed).mod(BigInteger.valueOf(n.toLong())).toInt()
        val out = mutableListOf<Int>()
        for (k in 0 until n) out.add((start + k) % n)
        return out
    }

    fun encode(plaintext: ByteArray): String {
        val aead = crypto.encrypt(plaintext)
        // Build payloadWithoutTemplate: version(1) | nonce(12) | tag(16) | plaintextLength(4) | ciphertext
        val baos = ByteArrayOutputStream()
        baos.write(byteArrayOf(aead.version.toByte()))
        baos.write(aead.nonce)
        baos.write(aead.tag)
        baos.write(intToBytes(aead.plaintextLength))
        baos.write(aead.ciphertext)
        val payloadWithoutTemplate = baos.toByteArray()

        // seed for deterministic ordering
        val seed = hmacSha256(mapKey, payloadWithoutTemplate)

        val N = BigInteger(1, payloadWithoutTemplate)

        // choose a template from a deterministic rotation starting at seed
        val order = rotationOrder(seed, templates.size)
        var chosenIndex: Int? = null
        for (idx in order) {
            val cap = bucketProduct(templates[idx])
            if (N < cap) { chosenIndex = idx; break }
        }
        if (chosenIndex == null) throw IllegalArgumentException("Payload does not fit into any template word space; increase bucket sizes or use more/longer templates")

        val templateIndex = chosenIndex

        // Incorporate templateIndex as the least-significant mixed-radix digit by forming N_combined = N * T + templateIndex
        val T = BigInteger.valueOf(templates.size.toLong())
        var Ncombined = N.multiply(T).add(BigInteger.valueOf(templateIndex.toLong()))

        // radices: first templates.size then bucket sizes so decomposition yields [templateIndex, slot0, slot1, ...]
        val selectedTemplate = templates[templateIndex]
        val radices = listOf(templates.size) + selectedTemplate.buckets.map { it.size }

        val digits = mutableListOf<Int>()
        for (r in radices) {
            val biR = BigInteger.valueOf(r.toLong())
            val digit = Ncombined.mod(biR).toInt()
            digits.add(digit)
            Ncombined = Ncombined.divide(biR)
        }
        if (Ncombined != BigInteger.ZERO) throw IllegalArgumentException("Payload does not fit into template word space after decomposition")

        // digits[0] is templateIndex, digits[1..] correspond to slots
        val slotDigits = digits.subList(1, 1 + selectedTemplate.slotCount())
        val words = slotDigits.mapIndexed { idx, d ->
            val bucket = selectedTemplate.buckets[idx]
            bucket[d]
        }
        return selectedTemplate.render(words)
    }

    fun decode(sentence: String): ByteArray {
        // try every template that matches the surface form
        for (t in templates) {
            val slots = t.matchSlots(sentence) ?: continue
            if (slots.size != t.slotCount()) continue
            // map words back to indices
            val indices = mutableListOf<Int>()
            var ok = true
            for (i in 0 until t.slotCount()) {
                val idx = t.buckets[i].indexOf(slots[i])
                if (idx < 0) { ok = false; break }
                indices.add(idx)
            }
            if (!ok) continue

            val templateIndex = templates.indexOf(t)
            // radices: first templates.size then bucket sizes
            val radices = listOf(templates.size) + t.buckets.map { it.size }

            // build digitsForCompose: [templateIndex, slotIndices...]
            val digitsForCompose = mutableListOf<Int>()
            digitsForCompose.add(templateIndex)
            digitsForCompose.addAll(indices)

            // compose Ncombined using mixed-radix little-endian
            var Ncombined = BigInteger.ZERO
            var multiplier = BigInteger.ONE
            for (i in digitsForCompose.indices) {
                val d = BigInteger.valueOf(digitsForCompose[i].toLong())
                Ncombined = Ncombined.add(d.multiply(multiplier))
                multiplier = multiplier.multiply(BigInteger.valueOf(radices[i].toLong()))
            }

            // recover original payload N = Ncombined / T
            val T = BigInteger.valueOf(templates.size.toLong())
            val N = Ncombined.divide(T)

            var payloadWithoutTemplate = N.toByteArray()
            if (payloadWithoutTemplate.size > 1 && payloadWithoutTemplate[0].toInt() == 0) {
                payloadWithoutTemplate = payloadWithoutTemplate.copyOfRange(1, payloadWithoutTemplate.size)
            }

            // payloadWithoutTemplate must be at least header length
            if (payloadWithoutTemplate.size < 33) continue

            // verify deterministic template selection matches PRF ordering used in encode
            val seed = hmacSha256(mapKey, payloadWithoutTemplate)
            val order = rotationOrder(seed, templates.size)
            // pick first template in order whose bucket product can hold N
            var expectedTemplateIndex: Int? = null
            for (idx in order) {
                val cap = bucketProduct(templates[idx])
                if (N < cap) { expectedTemplateIndex = idx; break }
            }
            if (expectedTemplateIndex == null) continue
            if (expectedTemplateIndex != templateIndex) continue

            // parse payloadWithoutTemplate: version(1) | nonce(12) | tag(16) | plaintextLength(4) | ciphertext
            val version = payloadWithoutTemplate[0].toInt()
            val nonce = payloadWithoutTemplate.copyOfRange(1, 13)
            val tag = payloadWithoutTemplate.copyOfRange(13, 29)
            val plaintextLen = bytesToInt(payloadWithoutTemplate, 29)
            val ciphertext = if (payloadWithoutTemplate.size > 33) payloadWithoutTemplate.copyOfRange(33, payloadWithoutTemplate.size) else ByteArray(0)

            val aead = AeadPayload(version = version, nonce = nonce, ciphertext = ciphertext, tag = tag, plaintextLength = plaintextLen)
            try {
                val plain = crypto.decrypt(aead)
                if (plain.size != plaintextLen) continue
                return plain
            } catch (_: Exception) {
                // decryption failed; try next template
                continue
            }
        }
        throw IllegalArgumentException("Unable to decode sentence: no matching template or failed integrity checks")
    }

    private fun intToBytes(v: Int): ByteArray {
        return byteArrayOf(
            ((v ushr 24) and 0xFF).toByte(),
            ((v ushr 16) and 0xFF).toByte(),
            ((v ushr 8) and 0xFF).toByte(),
            (v and 0xFF).toByte()
        )
    }

    private fun bytesToInt(arr: ByteArray, offset: Int): Int {
        return (arr[offset].toInt() and 0xFF shl 24) or
                (arr[offset + 1].toInt() and 0xFF shl 16) or
                (arr[offset + 2].toInt() and 0xFF shl 8) or
                (arr[offset + 3].toInt() and 0xFF)
    }
}
