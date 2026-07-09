package dev.alifhasnain.falconer.channel

/**
 * Typed channel-payload models — the Kotlin domain shape parsed from the wire
 * maps. Phase 4's Room entity is built by merging a [RequestPayload] with a
 * later [ResponsePayload] / [ErrorPayload] sharing the same [id].
 *
 * Nullability mirrors `doc/CHANNEL_CONTRACT.md`: required fields are non-null,
 * optional fields are nullable.
 */
data class RequestPayload(
    val id: String,
    val startedAt: Long,
    val method: String,
    val url: String,
    val host: String,
    val path: String,
    val scheme: String,
    val requestHeaders: Map<String, String>,
    val requestContentType: String?,
    val requestContentLength: Long?,
    val requestBody: String?,
    val requestBodyKind: String,
)

data class ResponsePayload(
    val id: String,
    val completedAt: Long,
    val tookMs: Long,
    val statusCode: Int?,
    val statusMessage: String?,
    val protocol: String?,
    val responseHeaders: Map<String, String>,
    val responseContentType: String?,
    val responseContentLength: Long?,
    val responseBody: String?,
    val responseBodyKind: String?,
    val responseImageBytes: ByteArray?,
) {
    // ByteArray needs structural equals/hashCode for value semantics.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResponsePayload) return false
        return id == other.id &&
            completedAt == other.completedAt &&
            tookMs == other.tookMs &&
            statusCode == other.statusCode &&
            statusMessage == other.statusMessage &&
            protocol == other.protocol &&
            responseHeaders == other.responseHeaders &&
            responseContentType == other.responseContentType &&
            responseContentLength == other.responseContentLength &&
            responseBody == other.responseBody &&
            responseBodyKind == other.responseBodyKind &&
            (responseImageBytes?.contentEquals(other.responseImageBytes) ?: (other.responseImageBytes == null))
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (statusCode ?: 0)
        result = 31 * result + (responseImageBytes?.contentHashCode() ?: 0)
        return result
    }
}

data class ErrorPayload(
    val id: String,
    val completedAt: Long,
    val tookMs: Long,
    val error: String,
)
