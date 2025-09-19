package waer.encoding

import waer.interfaces.TextEncoder
import waer.interfaces.EncryptedData
import java.math.BigInteger
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Template-based text encoder that converts binary data to human-readable text
 */
class TemplateTextEncoder(
    private val templates: List<Template>,
    private val mapKey: ByteArray
) : TextEncoder {

    override fun encode(encryptedData: EncryptedData): String {
        val payloadBytes = encryptedData.toByteArray()

        // Generate deterministic seed for template selection
        val seed = hmacSha256(mapKey, payloadBytes)
        val N = BigInteger(1, payloadBytes)

        // Choose template deterministically
        val order = rotationOrder(seed, templates.size)
        var chosenIndex: Int? = null
        for (idx in order) {
            val cap = bucketProduct(templates[idx])
            if (N < cap) {
                chosenIndex = idx
                break
            }
        }

        if (chosenIndex == null) {
            throw IllegalArgumentException("Payload does not fit into any template word space; increase bucket sizes or use more/longer templates")
        }

        val templateIndex = chosenIndex
        val selectedTemplate = templates[templateIndex]

        // Incorporate templateIndex as the least-significant mixed-radix digit
        val T = BigInteger.valueOf(templates.size.toLong())
        var Ncombined = N.multiply(T).add(BigInteger.valueOf(templateIndex.toLong()))

        // Convert to mixed-radix representation
        val radices = listOf(templates.size) + selectedTemplate.buckets.map { it.size }
        val digits = mutableListOf<Int>()

        for (r in radices) {
            val biR = BigInteger.valueOf(r.toLong())
            val digit = Ncombined.mod(biR).toInt()
            digits.add(digit)
            Ncombined = Ncombined.divide(biR)
        }

        if (Ncombined != BigInteger.ZERO) {
            throw IllegalArgumentException("Payload does not fit into template word space after decomposition")
        }

        // Map digits to words and render
        val slotDigits = digits.subList(1, 1 + selectedTemplate.slotCount())
        val words = slotDigits.mapIndexed { idx, d ->
            selectedTemplate.buckets[idx][d]
        }

        return selectedTemplate.render(words)
    }

    override fun decode(humanText: String): EncryptedData {
        // Try each template that matches the surface form
        for (template in templates) {
            val slots = template.matchSlots(humanText) ?: continue
            if (slots.size != template.slotCount()) continue

            // Map words back to indices
            val indices = mutableListOf<Int>()
            var valid = true

            for (i in 0 until template.slotCount()) {
                val idx = template.buckets[i].indexOf(slots[i])
                if (idx < 0) {
                    valid = false
                    break
                }
                indices.add(idx)
            }

            if (!valid) continue

            val templateIndex = templates.indexOf(template)
            val radices = listOf(templates.size) + template.buckets.map { it.size }

            // Reconstruct Ncombined using mixed-radix composition
            val digitsForCompose = mutableListOf<Int>()
            digitsForCompose.add(templateIndex)
            digitsForCompose.addAll(indices)

            var Ncombined = BigInteger.ZERO
            var multiplier = BigInteger.ONE

            for (i in digitsForCompose.indices) {
                val d = BigInteger.valueOf(digitsForCompose[i].toLong())
                Ncombined = Ncombined.add(d.multiply(multiplier))
                multiplier = multiplier.multiply(BigInteger.valueOf(radices[i].toLong()))
            }

            // Recover original payload
            val T = BigInteger.valueOf(templates.size.toLong())
            val N = Ncombined.divide(T)

            var payloadBytes = N.toByteArray()
            if (payloadBytes.size > 1 && payloadBytes[0].toInt() == 0) {
                payloadBytes = payloadBytes.copyOfRange(1, payloadBytes.size)
            }

            // Validate minimum size
            if (payloadBytes.size < 33) continue

            // Verify deterministic template selection
            val seed = hmacSha256(mapKey, payloadBytes)
            val order = rotationOrder(seed, templates.size)
            var expectedTemplateIndex: Int? = null

            for (idx in order) {
                val cap = bucketProduct(templates[idx])
                if (N < cap) {
                    expectedTemplateIndex = idx
                    break
                }
            }

            if (expectedTemplateIndex == null || expectedTemplateIndex != templateIndex) continue

            try {
                return EncryptedData.fromByteArray(payloadBytes)
            } catch (_: Exception) {
                continue
            }
        }

        throw IllegalArgumentException("Unable to decode sentence: no matching template or failed integrity checks")
    }

    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun bucketProduct(template: Template): BigInteger {
        var product = BigInteger.ONE
        for (bucketSize in template.buckets.map { it.size }) {
            product = product.multiply(BigInteger.valueOf(bucketSize.toLong()))
        }
        return product
    }

    private fun rotationOrder(seed: ByteArray, n: Int): List<Int> {
        val start = BigInteger(1, seed).mod(BigInteger.valueOf(n.toLong())).toInt()
        val result = mutableListOf<Int>()
        for (k in 0 until n) {
            result.add((start + k) % n)
        }
        return result
    }
}
