package dev.alifhasnain.falconer.notification

import dev.alifhasnain.falconer.data.HttpTransactionEntity
import kotlin.test.Test
import kotlin.test.assertEquals

internal class NotificationHelperTest {

    private fun tx(
        method: String = "GET",
        path: String = "/users",
        statusCode: Int? = null,
        error: String? = null,
    ) = HttpTransactionEntity(
        id = "1", startedAt = 0, completedAt = null, tookMs = null,
        method = method, url = "https://x$path", host = "x", path = path, scheme = "https",
        protocol = null, requestHeaders = "{}", requestContentType = null,
        requestContentLength = null, requestBody = null, requestBodyKind = "none",
        statusCode = statusCode, statusMessage = null, responseHeaders = null,
        responseContentType = null, responseContentLength = null, responseBody = null,
        responseBodyKind = null, responseImageBytes = null, error = error,
    )

    @Test
    fun line_showsStatusCode() {
        assertEquals("GET /users  200", NotificationHelper.line(tx(statusCode = 200)))
    }

    @Test
    fun line_showsErrForTransportError() {
        assertEquals("POST /pay  ERR", NotificationHelper.line(tx(method = "POST", path = "/pay", error = "timeout")))
    }

    @Test
    fun line_showsInFlightDots() {
        assertEquals("GET /users  ···", NotificationHelper.line(tx()))
    }

    @Test
    fun line_blankPathBecomesSlash() {
        assertEquals("GET /  204", NotificationHelper.line(tx(path = "", statusCode = 204)))
    }
}
