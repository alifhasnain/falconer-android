package dev.alifhasnain.falconer.channel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Golden-map tests for [PayloadMapper]. These fixtures mirror the maps built by
 * `buildRequestDto` / `buildResponseDto` / `buildErrorDto` on the Dart side
 * (see `test/contract/contract_test.dart`). Renaming or dropping a field on
 * either side breaks the matching golden test.
 */
internal class PayloadMapperTest {

    @Test
    fun parseRequest_goldenMap() {
        val map = hashMapOf<String, Any?>(
            "id" to "1700000000000-0",
            "startedAt" to 1_700_000_000_000L,
            "method" to "GET",
            "url" to "https://api.example.com/users?page=1",
            "host" to "api.example.com",
            "path" to "/users",
            "scheme" to "https",
            "requestHeaders" to hashMapOf("accept" to "application/json"),
            "requestContentType" to null,
            "requestContentLength" to null,
            "requestBody" to null,
            "requestBodyKind" to "none",
        )

        val p = PayloadMapper.parseRequest(map)

        assertEquals("1700000000000-0", p.id)
        assertEquals(1_700_000_000_000L, p.startedAt)
        assertEquals("GET", p.method)
        assertEquals("https://api.example.com/users?page=1", p.url)
        assertEquals("api.example.com", p.host)
        assertEquals("/users", p.path)
        assertEquals("https", p.scheme)
        assertEquals(mapOf("accept" to "application/json"), p.requestHeaders)
        assertNull(p.requestContentType)
        assertNull(p.requestContentLength)
        assertNull(p.requestBody)
        assertEquals("none", p.requestBodyKind)
    }

    @Test
    fun parseResponse_goldenMap_jsonBody() {
        val map = hashMapOf<String, Any?>(
            "id" to "1700000000000-0",
            "completedAt" to 1_700_000_000_120L,
            "tookMs" to 120, // small ints arrive as Int
            "statusCode" to 200,
            "statusMessage" to "OK",
            "protocol" to null,
            "responseHeaders" to hashMapOf("content-type" to "application/json"),
            "responseContentType" to "application/json",
            "responseContentLength" to 11L,
            "responseBody" to "{\"ok\":true}",
            "responseBodyKind" to "json",
            "responseImageBytes" to null,
        )

        val p = PayloadMapper.parseResponse(map)

        assertEquals("1700000000000-0", p.id)
        assertEquals(1_700_000_000_120L, p.completedAt)
        assertEquals(120L, p.tookMs)
        assertEquals(200, p.statusCode)
        assertEquals("OK", p.statusMessage)
        assertNull(p.protocol)
        assertEquals("application/json", p.responseContentType)
        assertEquals(11L, p.responseContentLength)
        assertEquals("{\"ok\":true}", p.responseBody)
        assertEquals("json", p.responseBodyKind)
        assertNull(p.responseImageBytes)
    }

    @Test
    fun parseResponse_goldenMap_imageBytes() {
        val png = byteArrayOf(137.toByte(), 80, 78, 71)
        val map = hashMapOf<String, Any?>(
            "id" to "1700000000000-1",
            "completedAt" to 1_700_000_000_200L,
            "tookMs" to 200L,
            "statusCode" to 200,
            "statusMessage" to null,
            "protocol" to null,
            "responseHeaders" to hashMapOf("content-type" to "image/png"),
            "responseContentType" to "image/png",
            "responseContentLength" to 4L,
            "responseBody" to null,
            "responseBodyKind" to "image",
            "responseImageBytes" to png,
        )

        val p = PayloadMapper.parseResponse(map)

        assertEquals("image", p.responseBodyKind)
        assertNull(p.responseBody)
        assertTrue(png.contentEquals(p.responseImageBytes))
    }

    @Test
    fun parseError_goldenMap() {
        val map = hashMapOf<String, Any?>(
            "id" to "1700000000000-2",
            "completedAt" to 1_700_000_001_000L,
            "tookMs" to 1000,
            "error" to "DioException: connectionTimeout",
        )

        val p = PayloadMapper.parseError(map)

        assertEquals("1700000000000-2", p.id)
        assertEquals(1_700_000_001_000L, p.completedAt)
        assertEquals(1000L, p.tookMs)
        assertEquals("DioException: connectionTimeout", p.error)
    }
}
