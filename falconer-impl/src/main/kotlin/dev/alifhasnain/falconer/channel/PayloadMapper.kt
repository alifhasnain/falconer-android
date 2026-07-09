package dev.alifhasnain.falconer.channel

/**
 * Parses the channel payload maps (as delivered by Flutter's
 * `StandardMessageCodec`) into typed [RequestPayload] / [ResponsePayload] /
 * [ErrorPayload] models.
 *
 * Required fields are read non-null; optional fields tolerate a missing key or
 * an explicit null. Numbers arrive as `Int` or `Long` depending on magnitude,
 * so they are read through [Number] and coerced.
 */
object PayloadMapper {

    fun parseRequest(map: Map<*, *>): RequestPayload = RequestPayload(
        id = map.string(PayloadKeys.ID),
        startedAt = map.long(PayloadKeys.STARTED_AT),
        method = map.string(PayloadKeys.METHOD),
        url = map.string(PayloadKeys.URL),
        host = map.string(PayloadKeys.HOST),
        path = map.string(PayloadKeys.PATH),
        scheme = map.string(PayloadKeys.SCHEME),
        requestHeaders = map.stringMap(PayloadKeys.REQUEST_HEADERS),
        requestContentType = map.stringOrNull(PayloadKeys.REQUEST_CONTENT_TYPE),
        requestContentLength = map.longOrNull(PayloadKeys.REQUEST_CONTENT_LENGTH),
        requestBody = map.stringOrNull(PayloadKeys.REQUEST_BODY),
        requestBodyKind = map.string(PayloadKeys.REQUEST_BODY_KIND),
    )

    fun parseResponse(map: Map<*, *>): ResponsePayload = ResponsePayload(
        id = map.string(PayloadKeys.ID),
        completedAt = map.long(PayloadKeys.COMPLETED_AT),
        tookMs = map.long(PayloadKeys.TOOK_MS),
        statusCode = map.intOrNull(PayloadKeys.STATUS_CODE),
        statusMessage = map.stringOrNull(PayloadKeys.STATUS_MESSAGE),
        protocol = map.stringOrNull(PayloadKeys.PROTOCOL),
        responseHeaders = map.stringMap(PayloadKeys.RESPONSE_HEADERS),
        responseContentType = map.stringOrNull(PayloadKeys.RESPONSE_CONTENT_TYPE),
        responseContentLength = map.longOrNull(PayloadKeys.RESPONSE_CONTENT_LENGTH),
        responseBody = map.stringOrNull(PayloadKeys.RESPONSE_BODY),
        responseBodyKind = map.stringOrNull(PayloadKeys.RESPONSE_BODY_KIND),
        responseImageBytes = map[PayloadKeys.RESPONSE_IMAGE_BYTES] as? ByteArray,
    )

    fun parseError(map: Map<*, *>): ErrorPayload = ErrorPayload(
        id = map.string(PayloadKeys.ID),
        completedAt = map.long(PayloadKeys.COMPLETED_AT),
        tookMs = map.long(PayloadKeys.TOOK_MS),
        error = map.string(PayloadKeys.ERROR),
    )

    // --- coercion helpers ----------------------------------------------------

    private fun Map<*, *>.string(key: String): String =
        this[key] as? String
            ?: throw IllegalArgumentException("Missing required String field '$key'")

    private fun Map<*, *>.stringOrNull(key: String): String? = this[key] as? String

    private fun Map<*, *>.long(key: String): Long =
        (this[key] as? Number)?.toLong()
            ?: throw IllegalArgumentException("Missing required numeric field '$key'")

    private fun Map<*, *>.longOrNull(key: String): Long? = (this[key] as? Number)?.toLong()

    private fun Map<*, *>.intOrNull(key: String): Int? = (this[key] as? Number)?.toInt()

    private fun Map<*, *>.stringMap(key: String): Map<String, String> {
        val raw = this[key] as? Map<*, *> ?: return emptyMap()
        return raw.entries.associate { (k, v) -> k.toString() to v.toString() }
    }
}
