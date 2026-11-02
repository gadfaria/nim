package com.gadfaria.nim.data

/**
 * Search ranking. Plain Kotlin, nothing Android, so it can be tested on the JVM
 * — the cases that matter ("wpp" finding WhatsApp, "cam" finding Câmera before
 * Scanner de Câmera) depend on apps the emulator doesn't have.
 */

/** Label already prepared for search. Computed once per app, not per keystroke. */
data class SearchableLabel(
    val normalized: String,
    val words: List<String>,
) {
    val initials: String = words.joinToString("") { it.take(1) }
}

// Lower is better. The order is the plan's: prefix > initials > contains.
//
// There was a fifth tier, subsequence, which made "wpp" find WhatsApp. It was
// removed on purpose: it dragged loose results into the list ("gm" brought
// Gemini via g...e...m) and, with them, auto-launch stopped holding — what's on
// screen has to be what opens.
private const val TIER_PREFIX = 0        // "cam" -> Câmera
private const val TIER_WORD_PREFIX = 1   // "cam" -> Scanner de Câmera
private const val TIER_INITIALS = 2      // "gm"  -> Google Maps
private const val TIER_CONTAINS = 3      // "ame" -> Câmera
const val NO_MATCH = Int.MAX_VALUE

private val NOT_LETTER_OR_DIGIT = Regex("[^\\p{L}\\p{N}]+")

/**
 * Splits into words on punctuation/whitespace and also on camelCase humps, so
 * "WhatsApp" yields ["whats", "app"] and the initials become "wa".
 */
fun searchableOf(label: String): SearchableLabel {
    val words = label.split(NOT_LETTER_OR_DIGIT)
        .flatMap(::splitCamelHumps)
        .filter { it.isNotEmpty() }
        .map(::normalizeForSearch)
    return SearchableLabel(normalized = normalizeForSearch(label), words = words)
}

/**
 * "WhatsApp" -> ["Whats", "App"]. Only splits when an uppercase letter follows a
 * lowercase one: acronyms like "YT" and "TV" stay whole.
 */
private fun splitCamelHumps(word: String): List<String> {
    if (word.length < 2) return listOf(word)
    val parts = mutableListOf<String>()
    var start = 0
    for (i in 1 until word.length) {
        if (word[i].isUpperCase() && word[i - 1].isLowerCase()) {
            parts += word.substring(start, i)
            start = i
        }
    }
    parts += word.substring(start)
    return parts
}

/** Which tier the label matches the term in, or [NO_MATCH]. */
fun matchTier(target: SearchableLabel, term: String): Int = when {
    term.isEmpty() -> TIER_PREFIX
    target.normalized.startsWith(term) -> TIER_PREFIX
    target.words.any { it.startsWith(term) } -> TIER_WORD_PREFIX
    target.initials.startsWith(term) -> TIER_INITIALS
    target.normalized.contains(term) -> TIER_CONTAINS
    else -> NO_MATCH
}

data class RankResult<T>(
    val items: List<T>,
    /**
     * The app to auto-launch: only exists when the search narrowed the list
     * down to one name. No picking among visible results — what's on screen is
     * what opens.
     */
    val unambiguous: T?,
)

fun <T> rankBySearch(
    items: List<T>,
    term: String,
    targetOf: (T) -> SearchableLabel,
): RankResult<T> {
    if (term.isEmpty()) return RankResult(items, null)

    val scored = items
        .map { it to matchTier(targetOf(it), term) }
        .filter { it.second != NO_MATCH }
        // sortedBy is stable: within the same tier the incoming alphabetical
        // order is preserved.
        .sortedBy { it.second }

    return RankResult(
        items = scored.map { it.first },
        unambiguous = scored.singleOrNull()?.first,
    )
}
