package dev.alifhasnain.falconer.export

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class CurlBuilderTest {

    @Test
    fun getWithNoBody_hasMethodUrlHeaders_noData() {
        val curl = CurlBuilder.build(
            txFixture(
                method = "GET",
                requestHeaders = mapOf("Accept" to "application/json"),
                requestBody = null,
                requestBodyKind = "none",
            ),
        )
        assertTrue(curl.startsWith("curl -X GET 'https://api.example.com/users'"))
        assertTrue(curl.contains("-H 'Accept: application/json'"))
        assertFalse(curl.contains("--data"))
    }

    @Test
    fun postJson_includesDataBody() {
        val curl = CurlBuilder.build(
            txFixture(method = "POST", requestBody = "{\"a\":1}", requestBodyKind = "json"),
        )
        assertTrue(curl.contains("curl -X POST"))
        assertTrue(curl.contains("--data '{\"a\":1}'"))
    }

    @Test
    fun redactedHeaderIsCarriedThroughNotSecrets() {
        val curl = CurlBuilder.build(
            txFixture(requestHeaders = mapOf("Authorization" to "**redacted**")),
        )
        assertTrue(curl.contains("-H 'Authorization: **redacted**'"))
    }

    @Test
    fun singleQuotesInBodyAreEscaped() {
        val curl = CurlBuilder.build(
            txFixture(method = "POST", requestBody = "a'b", requestBodyKind = "text"),
        )
        assertTrue(curl.contains("'a'\\''b'"))
    }

    @Test
    fun multipartBodyIsNotReproduced() {
        val curl = CurlBuilder.build(
            txFixture(method = "POST", requestBody = "field: x", requestBodyKind = "multipart"),
        )
        assertFalse(curl.contains("--data"))
    }
}
