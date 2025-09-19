package waer.encoding

import java.util.regex.Pattern

/**
 * Template for converting binary data to human-readable text patterns
 */
class Template(
    val id: Int,
    val patternParts: List<String>,
    val buckets: List<List<String>>
) {
    init {
        require(patternParts.size == buckets.size + 1) {
            "patternParts must be one more than buckets (text before/after each slot)"
        }
    }

    fun slotCount(): Int = buckets.size

    fun render(words: List<String>): String {
        require(words.size == slotCount()) { "Number of words must match slot count" }
        val sb = StringBuilder()
        for (i in 0 until slotCount()) {
            sb.append(patternParts[i])
            sb.append(words[i])
        }
        sb.append(patternParts[slotCount()])
        return sb.toString()
    }

    fun buildPattern(): Pattern {
        // Build regex that captures each slot as a group using \S+ (no whitespace inside tokens)
        val sb = StringBuilder()
        sb.append("^")
        for (i in 0 until slotCount()) {
            sb.append(Pattern.quote(patternParts[i]))
            sb.append("(\\S+)")
        }
        sb.append(Pattern.quote(patternParts[slotCount()]))
        sb.append("$")
        return Pattern.compile(sb.toString())
    }

    fun matchSlots(sentence: String): List<String>? {
        val pattern = buildPattern()
        val matcher = pattern.matcher(sentence)
        if (!matcher.matches()) return null

        val result = mutableListOf<String>()
        for (i in 1..slotCount()) {
            result.add(matcher.group(i))
        }
        return result
    }
}
