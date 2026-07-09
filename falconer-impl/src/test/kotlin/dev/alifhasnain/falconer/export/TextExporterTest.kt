package dev.alifhasnain.falconer.export

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TextExporterTest {

    @Test
    fun containsRequestAndResponseSections() {
        val out = TextExporter.build(txFixture())
        assertTrue(out.contains("GET https://api.example.com/users"))
        assertTrue(out.contains("Status: 200 OK"))
        assertTrue(out.contains("--- Request headers ---"))
        assertTrue(out.contains("Accept: application/json"))
        assertTrue(out.contains("--- Response body (json) ---"))
        assertTrue(out.contains("{\"ok\":true}"))
    }

    @Test
    fun imageResponseIsOmittedNotInlined() {
        val out = TextExporter.build(
            txFixture(
                responseBody = null,
                responseBodyKind = "image",
                responseImageBytes = byteArrayOf(1, 2, 3, 4),
            ),
        )
        assertTrue(out.contains("[image omitted]"))
        assertFalse(out.contains("[1, 2, 3, 4]"))
    }

    @Test
    fun transportErrorShown() {
        val out = TextExporter.build(
            txFixture(statusCode = null, statusMessage = null, error = "connectionTimeout"),
        )
        assertTrue(out.contains("Status: Failed"))
        assertTrue(out.contains("--- Error ---"))
        assertTrue(out.contains("connectionTimeout"))
    }

    @Test
    fun noBodyMarked() {
        val out = TextExporter.build(txFixture(requestBody = null, requestBodyKind = "none"))
        assertTrue(out.contains("--- Request body (none) ---"))
        assertTrue(out.contains("(no body)"))
    }
}
