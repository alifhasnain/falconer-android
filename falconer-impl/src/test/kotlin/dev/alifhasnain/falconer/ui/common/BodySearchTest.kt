package dev.alifhasnain.falconer.ui.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class BodySearchTest {

    @Test
    fun findMatches_isCaseInsensitiveAndNonOverlapping() {
        val matches = BodySearch.findMatches("aAaAa", "aa")
        // positions 0..1 and 2..3 (non-overlapping)
        assertEquals(listOf(0..1, 2..3), matches)
    }

    @Test
    fun findMatches_findsAllOccurrences() {
        val text = "token=abc; other=def; token=ghi"
        val matches = BodySearch.findMatches(text, "token")
        assertEquals(2, matches.size)
        assertEquals(0..4, matches.first())
    }

    @Test
    fun findMatches_emptyQueryOrText_isEmpty() {
        assertTrue(BodySearch.findMatches("hello", "").isEmpty())
        assertTrue(BodySearch.findMatches("", "x").isEmpty())
    }

    @Test
    fun findMatches_noMatch_isEmpty() {
        assertTrue(BodySearch.findMatches("hello world", "zzz").isEmpty())
    }
}
