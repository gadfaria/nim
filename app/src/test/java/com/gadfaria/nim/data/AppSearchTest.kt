package com.gadfaria.nim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * O ranqueamento roda na JVM porque os casos que importam dependem de apps que
 * o emulador não tem — não dá pra verificar "wpp" achando WhatsApp com adb.
 */
class AppSearchTest {

    private val apps = listOf(
        "Calendar", "Câmera", "Chrome", "Gmail", "Gemini",
        "Google Maps", "Scanner de Câmera", "WhatsApp", "YT Music",
    )

    private fun rank(term: String) =
        rankBySearch(apps, normalizeForSearch(term)) { searchableOf(it) }

    @Test
    fun `prefixo vem antes de prefixo de palavra`() {
        // O exemplo do plano: Câmera antes de Scanner de Câmera.
        assertEquals(listOf("Câmera", "Scanner de Câmera"), rank("cam").items)
    }

    @Test
    fun `busca ignora acento nos dois sentidos`() {
        assertEquals("Câmera", rank("camera").items.first())
        assertEquals("Câmera", rank("câmera").items.first())
    }

    @Test
    fun `iniciais acham nome composto`() {
        // "gm" são as iniciais de Google Maps — camada 2. Ele não vem primeiro
        // na lista porque Gmail casa como prefixo, que é mais forte.
        assertEquals(2, matchTier(searchableOf("Google Maps"), "gm"))
    }

    @Test
    fun `sem camada de subsequencia, wpp nao acha nada`() {
        // Decisão consciente: a subsequência achava WhatsApp por "wpp", mas
        // arrastava resultados frouxos e matava o abrir automático.
        assertEquals(emptyList<String>(), rank("wpp").items)
        assertEquals(listOf("WhatsApp"), rank("wha").items)
    }

    @Test
    fun `camelCase vira palavras separadas`() {
        // "app" é prefixo da segunda corcova de WhatsApp, não subsequência.
        assertEquals(1, matchTier(searchableOf("WhatsApp"), "app"))
    }

    @Test
    fun `sigla colada nao e quebrada`() {
        // "YT" é uma palavra só: "yt" casa como prefixo, tier 0.
        assertEquals(0, matchTier(searchableOf("YT Music"), "yt"))
    }

    @Test
    fun `gm nao traz Gemini e o Gmail vence Google Maps`() {
        // Gemini ficou de fora junto com a subsequência. Sobram Gmail
        // (prefixo, camada 0) e Google Maps (iniciais, camada 2).
        val result = rank("gm")
        assertEquals(listOf("Gmail", "Google Maps"), result.items)
    }

    @Test
    fun `so abre sozinho quando sobra um nome na tela`() {
        // Dois na lista: nada abre, mesmo com um deles sendo mais forte.
        assertNull(rank("gm").unambiguous)
        assertEquals("Gmail", rank("gma").unambiguous)
    }

    @Test
    fun `empate na melhor camada nao e inequivoco`() {
        // "c" é prefixo de Calendar, Câmera e Chrome.
        assertNull(rank("c").unambiguous)
    }

    @Test
    fun `busca sem resultado vem vazia`() {
        val result = rank("zzzz")
        assertEquals(emptyList<String>(), result.items)
        assertNull(result.unambiguous)
    }

    @Test
    fun `busca vazia nao dispara abertura`() {
        val result = rank("")
        assertEquals(apps, result.items)
        assertNull(result.unambiguous)
    }
}
