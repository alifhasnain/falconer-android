package dev.alifhasnain.falconer.notification

import dev.alifhasnain.falconer.data.TransactionListRow
import kotlin.test.Test
import kotlin.test.assertEquals

internal class NotificationHelperTest {

    private fun tx(
        method: String = "GET",
        path: String = "/users",
        statusCode: Int? = null,
        error: String? = null,
    ) = TransactionListRow(
        id = "1", startedAt = 0, tookMs = null,
        method = method, host = "x", path = path,
        statusCode = statusCode, responseContentLength = null, error = error,
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
