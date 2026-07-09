package dev.alifhasnain.falconer.channel

/**
 * Payload-map field keys — mirror of `PayloadKeys` in
 * `lib/src/platform/contract.dart`. Renaming a key here must be matched on the
 * Dart side; the golden tests on both sides fail on drift.
 */
object PayloadKeys {
    // Shared.
    const val ID = "id"

    // logRequest.
    const val STARTED_AT = "startedAt"
    const val METHOD = "method"
    const val URL = "url"
    const val HOST = "host"
    const val PATH = "path"
    const val SCHEME = "scheme"
    const val REQUEST_HEADERS = "requestHeaders"
    const val REQUEST_CONTENT_TYPE = "requestContentType"
    const val REQUEST_CONTENT_LENGTH = "requestContentLength"
    const val REQUEST_BODY = "requestBody"
    const val REQUEST_BODY_KIND = "requestBodyKind"

    // logResponse.
    const val COMPLETED_AT = "completedAt"
    const val TOOK_MS = "tookMs"
    const val STATUS_CODE = "statusCode"
    const val STATUS_MESSAGE = "statusMessage"
    const val PROTOCOL = "protocol"
    const val RESPONSE_HEADERS = "responseHeaders"
    const val RESPONSE_CONTENT_TYPE = "responseContentType"
    const val RESPONSE_CONTENT_LENGTH = "responseContentLength"
    const val RESPONSE_BODY = "responseBody"
    const val RESPONSE_BODY_KIND = "responseBodyKind"
    const val RESPONSE_IMAGE_BYTES = "responseImageBytes"

    // logError.
    const val ERROR = "error"
}
