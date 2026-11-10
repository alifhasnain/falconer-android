package dev.alifhasnain.falconer.data

import dev.alifhasnain.falconer.channel.ErrorPayload
import dev.alifhasnain.falconer.channel.RequestPayload
import dev.alifhasnain.falconer.channel.ResponsePayload
import dev.alifhasnain.falconer.config.FalconerNativeConfig
import kotlinx.coroutines.flow.Flow

/**
 * Writes captured transactions into Room: insert on request, merge-update on
 * response/error (D4). After each write it runs throttled retention cleanup.
 *
 * [config] supplies the current [FalconerNativeConfig], used to re-apply
 * redaction and truncation as a backstop before persisting.
 */
class TransactionRepository(
    private val dao: HttpTransactionDao,
    private val retention: RetentionManager,
    private val config: () -> FalconerNativeConfig = { FalconerNativeConfig.DEFAULT },
) {
    fun observeListRowsFiltered(query: String): Flow<List<TransactionListRow>> =
        dao.observeListRowsFiltered(query)
    fun observeRecent(limit: Long): Flow<List<TransactionListRow>> = dao.observeRecent(limit)
    fun observeById(id: String): Flow<HttpTransactionEntity?> = dao.observeById(id)
    fun observeCount(): Flow<Int> = dao.observeCount()

    suspend fun logRequest(payload: RequestPayload) {
        dao.insert(payload.toEntity())
        retention.onWrite()
    }

    suspend fun logResponse(payload: ResponsePayload) {
        val cfg = config()
        val imageBytes = payload.responseImageBytes
        val imageOverCap = imageBytes != null && imageBytes.size > cfg.maxContentLength
        val existing = dao.findById(payload.id)
        val merged = (existing ?: skeleton(payload.id)).copy(
            completedAt = payload.completedAt,
            tookMs = payload.tookMs,
            statusCode = payload.statusCode,
            statusMessage = payload.statusMessage,
            protocol = payload.protocol,
            responseHeaders = Converters.headersToJson(cfg.redact(payload.responseHeaders)),
            responseContentType = payload.responseContentType,
            responseContentLength = payload.responseContentLength,
            responseBody = if (imageOverCap) {
                "[Falconer: image truncated — ${imageBytes!!.size} bytes > ${cfg.maxContentLength}]"
            } else {
                cfg.truncate(payload.responseBody, payload.responseContentLength)
            },
            responseBodyKind = payload.responseBodyKind,
            responseImageBytes = if (imageOverCap) null else imageBytes,
            error = null,
        )
        if (existing != null) dao.update(merged) else dao.insert(merged)
        retention.onWrite()
    }

    suspend fun logError(payload: ErrorPayload) {
        val existing = dao.findById(payload.id)
        val merged = (existing ?: skeleton(payload.id)).copy(
            completedAt = payload.completedAt,
            tookMs = payload.tookMs,
            error = payload.error,
        )
        if (existing != null) dao.update(merged) else dao.insert(merged)
        retention.onWrite()
    }

    suspend fun clear() = dao.clear()

    /** Runs an unconditional retention pass (startup / configure). */
    suspend fun cleanupOnStart() = retention.cleanupNow()

    private fun RequestPayload.toEntity(): HttpTransactionEntity {
        val cfg = config()
        return HttpTransactionEntity(
            id = id,
            startedAt = startedAt,
            completedAt = null,
            tookMs = null,
            method = method,
            url = url,
            host = host,
            path = path,
            scheme = scheme,
            protocol = null,
            requestHeaders = Converters.headersToJson(cfg.redact(requestHeaders)),
            requestContentType = requestContentType,
            requestContentLength = requestContentLength,
            requestBody = cfg.truncate(requestBody, requestContentLength),
            requestBodyKind = requestBodyKind,
            statusCode = null,
            statusMessage = null,
            responseHeaders = null,
            responseContentType = null,
            responseContentLength = null,
            responseBody = null,
            responseBodyKind = null,
            responseImageBytes = null,
            error = null,
        )
    }

    // Fallback row when a response/error arrives with no matching request
    // (e.g. the request insert failed). Keeps the data rather than dropping it.
    private fun skeleton(id: String) = HttpTransactionEntity(
        id = id,
        startedAt = retentionNow(),
        completedAt = null,
        tookMs = null,
        method = "",
        url = "",
        host = "",
        path = "",
        scheme = "",
        protocol = null,
        requestHeaders = Converters.headersToJson(emptyMap()),
        requestContentType = null,
        requestContentLength = null,
        requestBody = null,
        requestBodyKind = "none",
        statusCode = null,
        statusMessage = null,
        responseHeaders = null,
        responseContentType = null,
        responseContentLength = null,
        responseBody = null,
        responseBodyKind = null,
        responseImageBytes = null,
        error = null,
    )

    private fun retentionNow(): Long = System.currentTimeMillis()
}
