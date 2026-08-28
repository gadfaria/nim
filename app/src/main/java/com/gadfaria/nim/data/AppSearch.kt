package com.gadfaria.nim.data

/**
 * Ranqueamento da busca. Kotlin puro, sem nada de Android, para poder ser
 * testado na JVM — os casos que importam ("wpp" achar WhatsApp, "cam" achar
 * Câmera antes de Scanner de Câmera) dependem de apps que o emulador não tem.
 */

/** Rótulo já preparado para busca. Calculado uma vez por app, não por tecla. */
data class SearchableLabel(
    val normalized: String,
    val words: List<String>,
) {
    val initials: String = words.joinToString("") { it.take(1) }
}

// Menor é melhor. A ordem é a do plano: prefixo > iniciais > contém, com
// subsequência no fim como rede de segurança.
private const val TIER_PREFIX = 0        // "cam" -> Câmera
private const val TIER_WORD_PREFIX = 1   // "cam" -> Scanner de Câmera
private const val TIER_INITIALS = 2      // "gm"  -> Google Maps
private const val TIER_CONTAINS = 3      // "ame" -> Câmera
private const val TIER_SUBSEQUENCE = 4   // "wpp" -> WhatsApp
const val NO_MATCH = Int.MAX_VALUE

private val NOT_LETTER_OR_DIGIT = Regex("[^\\p{L}\\p{N}]+")

/**
 * Quebra em palavras por pontuação/espaço e também nas corcovas de camelCase,
 * para que "WhatsApp" renda ["whats", "app"] e as iniciais fiquem "wa".
 */
fun searchableOf(label: String): SearchableLabel {
    val words = label.split(NOT_LETTER_OR_DIGIT)
        .flatMap(::splitCamelHumps)
        .filter { it.isNotEmpty() }
        .map(::normalizeForSearch)
    return SearchableLabel(normalized = normalizeForSearch(label), words = words)
}

/**
 * "WhatsApp" -> ["Whats", "App"]. Só quebra quando uma maiúscula vem depois de
 * minúscula: siglas como "YT" e "TV" continuam inteiras.
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

/** Em que camada o rótulo casa com o termo, ou [NO_MATCH]. */
fun matchTier(target: SearchableLabel, term: String): Int = when {
    term.isEmpty() -> TIER_PREFIX
    target.normalized.startsWith(term) -> TIER_PREFIX
    target.words.any { it.startsWith(term) } -> TIER_WORD_PREFIX
    target.initials.startsWith(term) -> TIER_INITIALS
    target.normalized.contains(term) -> TIER_CONTAINS
    isSubsequence(target.normalized, term) -> TIER_SUBSEQUENCE
    else -> NO_MATCH
}

/** As letras do termo aparecem em ordem no rótulo, não necessariamente juntas. */
private fun isSubsequence(text: String, term: String): Boolean {
    var i = 0
    for (char in text) {
        if (char == term[i]) {
            i++
            if (i == term.length) return true
        }
    }
    return false
}

data class RankResult<T>(
    val items: List<T>,
    /**
     * O único app da melhor camada, se ela tiver exatamente um. É isto que
     * dispara o abrir automático — e não "a lista toda tem um item só", que
     * deixaria de acontecer assim que uma subsequência frouxa entrasse na lista.
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
        // sortedBy é estável: dentro da mesma camada a ordem alfabética de
        // entrada é preservada.
        .sortedBy { it.second }

    val bestTier = scored.firstOrNull()?.second
    val best = scored.filter { it.second == bestTier }

    return RankResult(
        items = scored.map { it.first },
        unambiguous = best.singleOrNull()?.first,
    )
}
