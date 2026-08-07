package dev.alifhasnain.falconer.data

/**
 * The narrow projection behind the list screen and the ongoing notification.
 *
 * Everything these two surfaces render, and nothing else — no headers, no request
 * or response body, no image BLOB. [HttpTransactionEntity] stays the full row and
 * is loaded one at a time by the detail screen, which is the only place that
 * actually shows a payload.
 *
 * `responseContentLength` is the *declared* size from the response, so the list
 * can show a size without holding the bytes.
 */
data class TransactionListRow(
    val id: String,
    val startedAt: Long,
    val tookMs: Long?,
    val method: String,
    val host: String,
    val path: String,
    val statusCode: Int?,
    val responseContentLength: Long?,
    val error: String?,
)
