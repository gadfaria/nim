package com.gadfaria.nim.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoritesTest {

    @Test
    fun `round trip keeps the favoriting order`() {
        val keys = listOf("com.b/.Main#0", "com.a/.Main#0", "shortcut:com.c/site#10")
        assertEquals(keys, decodeFavorites(encodeFavorites(keys)))
    }

    @Test
    fun `nothing stored decodes to an empty list`() {
        assertEquals(emptyList<String>(), decodeFavorites(null))
        assertEquals(emptyList<String>(), decodeFavorites(""))
    }

    @Test
    fun `unescaped data from older versions still decodes`() {
        assertEquals(
            listOf("com.a/.Main#0", "com.b/.Main#0"),
            decodeFavorites("com.a/.Main#0\ncom.b/.Main#0"),
        )
    }

    @Test
    fun `a line break in a shortcut id stays inside its key`() {
        val keys = listOf("shortcut:com.c/one\ncom.bank/.Main#0", "com.a/.Main#0")
        assertEquals(keys, decodeFavorites(encodeFavorites(keys)))
    }

    @Test
    fun `backslashes survive the round trip`() {
        val keys = listOf("shortcut:com.c/a\\nb#0", "shortcut:com.c/tail\\#0")
        assertEquals(keys, decodeFavorites(encodeFavorites(keys)))
    }

    @Test
    fun `toggling adds at the end and removes in place`() {
        assertEquals(listOf("a", "b", "c"), toggleFavorite(listOf("a", "b"), "c"))
        assertEquals(listOf("a", "c"), toggleFavorite(listOf("a", "b", "c"), "b"))
    }
}
