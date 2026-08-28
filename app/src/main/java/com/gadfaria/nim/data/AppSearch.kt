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

// Menor é melhor. A ordem é a do plano: prefixo > iniciais > contém.
//
// Houve uma quinta camada, de subsequência, que fazia "wpp" achar WhatsApp.
// Foi removida de propósito: ela arrastava resultados frouxos para a lista
// ("gm" trazia Gemini por g...e...m) e, com eles, o abrir automático deixava
// de valer — o que está na tela tem que ser o que abre.
private const val TIER_PREFIX = 0        // "cam" -> Câmera
private const val TIER_WORD_PREFIX = 1   // "cam" -> Scanner de Câmera
private const val TIER_INITIALS = 2      // "gm"  -> Google Maps
private const val TIER_CONTAINS = 3      // "ame" -> Câmera
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
    else -> NO_MATCH
}

data class RankResult<T>(
    val items: List<T>,
    /**
     * O app a abrir sozinho: existe só quando a busca reduziu a lista a um
     * nome. Nada de escolher entre resultados visíveis — o que está na tela
     * é o que abre.
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

    return RankResult(
        items = scored.map { it.first },
        unambiguous = scored.singleOrNull()?.first,
    )
}
