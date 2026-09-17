package com.r2dapps.cutemascot

/**
 * Builds a timed phoneme schedule from text + total duration.
 * Port of Python build_phoneme_timeline() from desktop app.py.
 *
 * Returns list of Triple(startSec, endSec, phonemeKey)
 */
object PhonemeTimeline {

    // Telugu Unicode vowel ranges
    private val teluguA = setOf(0x0C05, 0x0C06, 0x0C3E)
    private val teluguI = setOf(0x0C07, 0x0C08, 0x0C3F, 0x0C40)
    private val teluguU = setOf(0x0C09, 0x0C0A, 0x0C41, 0x0C42)
    private val teluguE = setOf(0x0C0E, 0x0C0F, 0x0C10, 0x0C46, 0x0C47, 0x0C48)
    private val teluguO = setOf(0x0C12, 0x0C13, 0x0C14, 0x0C4A, 0x0C4B, 0x0C4C)
    private val teluguM = setOf(0x0C2E, 0x0C2C, 0x0C2A, 0x0C2D, 0x0C2B)

    fun build(text: String, totalDuration: Float): List<Triple<Float, Float, String>> {
        val tokens = mutableListOf<Pair<String, Float>>()

        for (ch in text) {
            val code = ch.code
            when {
                ch in " ,.!?~-…\n" -> tokens += "M" to if (ch in ",.!?~…\n") 2.2f else 1.2f
                code in 0x0C00..0x0C7F -> tokens += when (code) {
                    in teluguA -> "A" to 1.6f
                    in teluguI -> "I" to 1.3f
                    in teluguU -> "U" to 1.4f
                    in teluguE -> "E" to 1.4f
                    in teluguO -> "O" to 1.5f
                    in teluguM -> "M" to 1.1f
                    else       -> "E" to 1.0f
                }
                else -> {
                    val c = ch.lowercaseChar()
                    tokens += when {
                        c == 'a'            -> "A" to 1.5f
                        c == 'o'            -> "O" to 1.4f
                        c == 'u'            -> "U" to 1.4f
                        c == 'e'            -> "E" to 1.3f
                        c == 'i'            -> "I" to 1.2f
                        c in "mbp"          -> "M" to 1.0f
                        c in "sztdjckn"     -> "I" to 1.0f
                        else                -> "E" to 1.0f
                    }
                }
            }
        }

        if (tokens.isEmpty()) return listOf(Triple(0f, totalDuration, "M"))

        // Compact adjacent identical phonemes
        val compacted = mutableListOf<Pair<String, Float>>()
        for ((p, w) in tokens) {
            if (compacted.isNotEmpty() && compacted.last().first == p) {
                compacted[compacted.lastIndex] = p to (compacted.last().second + w * 0.75f)
            } else {
                compacted += p to w
            }
        }

        val totalWeight = compacted.sumOf { it.second.toDouble() }.toFloat()
        val timeline = mutableListOf<Triple<Float, Float, String>>()
        var cur = 0f
        for ((p, w) in compacted) {
            val dur = (w / totalWeight) * totalDuration.coerceAtLeast(0.5f)
            timeline += Triple(cur, cur + dur, p)
            cur += dur
        }
        return timeline
    }
}
