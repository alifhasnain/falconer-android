package dev.alifhasnain.falconer.data

/**
 * One captured HTTP transaction (request merged with its response or error).
 *
 * Plain domain model — the persistence seam. SQLDelight generates its own row
 * type from `Transactions.sq`; [SqlDelightHttpTransactionDao] maps that row to
 * this class so callers never depend on generated code. Headers are stored as
 * JSON strings (see [Converters]); the response image is a BLOB in the row (D5),
 * hard-capped by `maxContentLength` in Phase 7.
 */
data class HttpTransactionEntity(
    val id: String,
    val startedAt: Long,
    val completedAt: Long?,
    val tookMs: Long?,
    val method: String,
    val url: String,
    val host: String,
    val path: String,
    val scheme: String,
    val protocol: String?,
    val requestHeaders: String,
    val requestContentType: String?,
    val requestContentLength: Long?,
    val requestBody: String?,
    val requestBodyKind: String,
    val statusCode: Int?,
    val statusMessage: String?,
    val responseHeaders: String?,
    val responseContentType: String?,
    val responseContentLength: Long?,
    val responseBody: String?,
    val responseBodyKind: String?,
    val responseImageBytes: ByteArray?,
    val error: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HttpTransactionEntity) return false
        if (id != other.id) return false
        if (responseImageBytes != null) {
            if (other.responseImageBytes == null) return false
            if (!responseImageBytes.contentEquals(other.responseImageBytes)) return false
        } else if (other.responseImageBytes != null) {
            return false
        }
        return startedAt == other.startedAt &&
            completedAt == other.completedAt &&
            tookMs == other.tookMs &&
            method == other.method &&
            url == other.url &&
            host == other.host &&
            path == other.path &&
            scheme == other.scheme &&
            protocol == other.protocol &&
            requestHeaders == other.requestHeaders &&
            requestContentType == other.requestContentType &&
            requestContentLength == other.requestContentLength &&
            requestBody == other.requestBody &&
            requestBodyKind == other.requestBodyKind &&
            statusCode == other.statusCode &&
            statusMessage == other.statusMessage &&
            responseHeaders == other.responseHeaders &&
            responseContentType == other.responseContentType &&
            responseContentLength == other.responseContentLength &&
            responseBody == other.responseBody &&
            responseBodyKind == other.responseBodyKind &&
            error == other.error
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + startedAt.hashCode()
        result = 31 * result + (statusCode ?: 0)
        result = 31 * result + (responseImageBytes?.contentHashCode() ?: 0)
        return result
    }
}
