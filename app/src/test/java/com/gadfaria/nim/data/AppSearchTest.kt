package com.gadfaria.nim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Ranking runs on the JVM because the cases that matter depend on apps the
 * emulator doesn't have — you can't verify "wpp" finding WhatsApp with adb.
 */
class AppSearchTest {

    private val apps = listOf(
        "Calendar", "Câmera", "Chrome", "Gmail", "Gemini",
        "Google Maps", "Scanner de Câmera", "WhatsApp", "YT Music",
    )

    private fun rank(term: String) =
        rankBySearch(apps, normalizeForSearch(term)) { searchableOf(it) }

    @Test
    fun `prefix comes before word prefix`() {
        // The plan's example: Câmera before Scanner de Câmera.
        assertEquals(listOf("Câmera", "Scanner de Câmera"), rank("cam").items)
    }

    @Test
    fun `search ignores accents both ways`() {
        assertEquals("Câmera", rank("camera").items.first())
        assertEquals("Câmera", rank("câmera").items.first())
    }

    @Test
    fun `initials find compound names`() {
        // "gm" are Google Maps' initials — tier 2. It doesn't come first in the
        // list because Gmail matches as a prefix, which is stronger.
        assertEquals(2, matchTier(searchableOf("Google Maps"), "gm"))
    }

    @Test
    fun `without the subsequence tier, wpp finds nothing`() {
        // Deliberate decision: subsequence found WhatsApp via "wpp", but it
        // dragged in loose results and killed auto-launch.
        assertEquals(emptyList<String>(), rank("wpp").items)
        assertEquals(listOf("WhatsApp"), rank("wha").items)
    }

    @Test
    fun `camelCase becomes separate words`() {
        // "app" is a prefix of WhatsApp's second hump, not a subsequence.
        assertEquals(1, matchTier(searchableOf("WhatsApp"), "app"))
    }

    @Test
    fun `joined acronym is not split`() {
        // "YT" is a single word: "yt" matches as a prefix, tier 0.
        assertEquals(0, matchTier(searchableOf("YT Music"), "yt"))
    }

    @Test
    fun `gm does not bring Gemini and Gmail beats Google Maps`() {
        // Gemini went away along with subsequence. What's left is Gmail
        // (prefix, tier 0) and Google Maps (initials, tier 2).
        val result = rank("gm")
        assertEquals(listOf("Gmail", "Google Maps"), result.items)
    }

    @Test
    fun `only auto-launches when one name is left on screen`() {
        // Two in the list: nothing opens, even with one of them being stronger.
        assertNull(rank("gm").unambiguous)
        assertEquals("Gmail", rank("gma").unambiguous)
    }

    @Test
    fun `tie in the best tier is not unambiguous`() {
        // "c" is a prefix of Calendar, Câmera and Chrome.
        assertNull(rank("c").unambiguous)
    }

    @Test
    fun `search with no result comes back empty`() {
        val result = rank("zzzz")
        assertEquals(emptyList<String>(), result.items)
        assertNull(result.unambiguous)
    }

    @Test
    fun `empty search does not trigger a launch`() {
        val result = rank("")
        assertEquals(apps, result.items)
        assertNull(result.unambiguous)
    }
}
