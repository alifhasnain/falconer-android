package dev.alifhasnain.falconer.data

import kotlin.test.Test
import kotlin.test.assertEquals

internal class ConvertersTest {

    @Test
    fun headers_roundTrip_preservesSpecialChars() {
        val headers = mapOf(
            "Content-Type" to "application/json",
            "x-multi" to "a, b, c",
            "tricky" to "\"quoted\" and \n newline",
        )

        val json = Converters.headersToJson(headers)

        assertEquals(headers, Converters.jsonToHeaders(json))
    }

    @Test
    fun jsonToHeaders_nullOrEmpty_isEmptyMap() {
        assertEquals(emptyMap<String, String>(), Converters.jsonToHeaders(null))
        assertEquals(emptyMap<String, String>(), Converters.jsonToHeaders(""))
    }

    @Test
    fun jsonToHeaders_malformed_isEmptyMap() {
        assertEquals(emptyMap<String, String>(), Converters.jsonToHeaders("{not valid json"))
    }
}
