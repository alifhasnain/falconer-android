package dev.alifhasnain.falconer.export

import dev.alifhasnain.falconer.data.Converters
import dev.alifhasnain.falconer.data.HttpTransactionEntity

/** Builds a transaction entity for export tests. */
internal fun txFixture(
    method: String = "GET",
    url: String = "https://api.example.com/users",
    requestHeaders: Map<String, String> = mapOf("Accept" to "application/json"),
    requestBody: String? = null,
    requestBodyKind: String = "none",
    statusCode: Int? = 200,
    statusMessage: String? = "OK",
    responseHeaders: Map<String, String> = mapOf("content-type" to "application/json"),
    responseBody: String? = "{\"ok\":true}",
    responseBodyKind: String? = "json",
    responseImageBytes: ByteArray? = null,
    error: String? = null,
) = HttpTransactionEntity(
    id = "1",
    startedAt = 1_700_000_000_000L,
    completedAt = 1_700_000_000_120L,
    tookMs = 120,
    method = method,
    url = url,
    host = "api.example.com",
    path = "/users",
    scheme = "https",
    protocol = null,
    requestHeaders = Converters.headersToJson(requestHeaders),
    requestContentType = null,
    requestContentLength = null,
    requestBody = requestBody,
    requestBodyKind = requestBodyKind,
    statusCode = statusCode,
    statusMessage = statusMessage,
    responseHeaders = Converters.headersToJson(responseHeaders),
    responseContentType = "application/json",
    responseContentLength = null,
    responseBody = responseBody,
    responseBodyKind = responseBodyKind,
    responseImageBytes = responseImageBytes,
    error = error,
)
