package dev.alifhasnain.falconer.ui.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class JsonFormatterTest {

    @Test
    fun prettyPrintsValidJsonObject() {
        val out = JsonFormatter.format("""{"a":1,"b":{"c":2}}""")
        assertTrue(out.contains("\n"), "expected multi-line output")
        assertTrue(out.contains("\"a\": 1"))
    }

    @Test
    fun prettyPrintsValidJsonArray() {
        val out = JsonFormatter.format("[1,2,3]")
        assertTrue(out.contains("\n"))
        assertTrue(out.trim().startsWith("["))
    }

    @Test
    fun malformedJsonReturnedUnchanged() {
        val raw = "{not valid json"
        assertEquals(raw, JsonFormatter.format(raw))
    }

    @Test
    fun nullOrBlankIsEmpty() {
        assertEquals("", JsonFormatter.format(null))
        assertEquals("", JsonFormatter.format("   "))
    }
}
