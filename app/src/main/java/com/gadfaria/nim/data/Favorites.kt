package com.gadfaria.nim.data

/**
 * How the favorites list goes to disk: one key per line, in favoriting order.
 * Plain Kotlin so it can be tested on the JVM.
 *
 * Keys are escaped because a shortcut key carries the shortcut id, which its
 * owner app picks freely — a line break in it would split one favorite in two.
 */

private const val SEPARATOR = '\n'
private const val ESCAPE = '\\'

fun encodeFavorites(keys: List<String>): String =
    keys.joinToString(SEPARATOR.toString(), transform = ::escape)

fun decodeFavorites(stored: String?): List<String> =
    stored?.split(SEPARATOR)?.filter { it.isNotEmpty() }?.map(::unescape).orEmpty()

/** Adds the key at the end, or removes it if it's already there. */
fun toggleFavorite(keys: List<String>, key: String): List<String> =
    if (key in keys) keys - key else keys + key

private fun escape(key: String): String =
    key.replace("\\", "\\\\").replace("\n", "\\n")

private fun unescape(line: String): String {
    if (ESCAPE !in line) return line
    val out = StringBuilder(line.length)
    var i = 0
    while (i < line.length) {
        val next = line.getOrNull(i + 1)
        when {
            line[i] != ESCAPE -> out.append(line[i])
            next == 'n' -> { out.append('\n'); i++ }
            next == ESCAPE -> { out.append(ESCAPE); i++ }
            // A lone backslash was written before keys were escaped: keep it.
            else -> out.append(ESCAPE)
        }
        i++
    }
    return out.toString()
}
