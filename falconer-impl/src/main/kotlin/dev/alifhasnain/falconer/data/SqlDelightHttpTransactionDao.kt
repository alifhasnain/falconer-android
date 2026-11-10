package dev.alifhasnain.falconer.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Production [HttpTransactionDao] backed by SQLDelight's generated
 * [TransactionsQueries]. Suspending writes/one-shot reads run on [io]; the
 * `observe*` Flows re-emit whenever the `transactions` table changes (SQLDelight
 * per-table listeners — the equivalent of Room's InvalidationTracker).
 *
 * Maps the generated [Transactions] row to the domain [HttpTransactionEntity] so
 * no caller depends on generated code.
 */
internal class SqlDelightHttpTransactionDao(
    private val queries: TransactionsQueries,
    private val io: CoroutineContext = Dispatchers.IO,
) : HttpTransactionDao {

    override suspend fun insert(transaction: HttpTransactionEntity) {
        withContext(io) {
            queries.insertOrReplace(
                id = transaction.id,
                startedAt = transaction.startedAt,
                completedAt = transaction.completedAt,
                tookMs = transaction.tookMs,
                method = transaction.method,
                url = transaction.url,
                host = transaction.host,
                path = transaction.path,
                scheme = transaction.scheme,
                protocol = transaction.protocol,
                requestHeaders = transaction.requestHeaders,
                requestContentType = transaction.requestContentType,
                requestContentLength = transaction.requestContentLength,
                requestBody = transaction.requestBody,
                requestBodyKind = transaction.requestBodyKind,
                statusCode = transaction.statusCode?.toLong(),
                statusMessage = transaction.statusMessage,
                responseHeaders = transaction.responseHeaders,
                responseContentType = transaction.responseContentType,
                responseContentLength = transaction.responseContentLength,
                responseBody = transaction.responseBody,
                responseBodyKind = transaction.responseBodyKind,
                responseImageBytes = transaction.responseImageBytes,
                error = transaction.error,
            )
        }
    }

    // INSERT OR REPLACE is a full-row replace by primary key, identical to a
    // whole-row Room @Update.
    override suspend fun update(transaction: HttpTransactionEntity) = insert(transaction)

    override suspend fun findById(id: String): HttpTransactionEntity? = withContext(io) {
        queries.findById(id).executeAsOneOrNull()?.toEntity()
    }

    override fun observeListRows(): Flow<List<TransactionListRow>> =
        queries.observeListRows(::listRow).asFlow().mapToList(io)

    override fun observeListRowsFiltered(query: String): Flow<List<TransactionListRow>> =
        queries.observeListRowsFiltered(query, ::listRow).asFlow().mapToList(io)

    override fun observeRecent(limit: Long): Flow<List<TransactionListRow>> =
        queries.observeRecent(limit, ::listRow).asFlow().mapToList(io)

    override fun observeById(id: String): Flow<HttpTransactionEntity?> =
        queries.observeById(id).asFlow().mapToOneOrNull(io).map { it?.toEntity() }

    override fun observeCount(): Flow<Int> =
        queries.observeCount().asFlow().mapToOne(io).map { it.toInt() }

    override suspend fun clear() {
        withContext(io) { queries.clear() }
    }

    override suspend fun deleteBefore(threshold: Long) {
        withContext(io) { queries.deleteBefore(threshold) }
    }
}

/**
 * Mapper for the projected list queries. The parameter list must match the SELECT
 * column order in `Transactions.sq` — SQLDelight passes them positionally, and
 * `statusCode` arrives as the column's `Long?` before narrowing.
 */
private fun listRow(
    id: String,
    startedAt: Long,
    tookMs: Long?,
    method: String,
    host: String,
    path: String,
    statusCode: Long?,
    responseContentLength: Long?,
    error: String?,
) = TransactionListRow(
    id = id,
    startedAt = startedAt,
    tookMs = tookMs,
    method = method,
    host = host,
    path = path,
    statusCode = statusCode?.toInt(),
    responseContentLength = responseContentLength,
    error = error,
)

/** Maps a generated SQLDelight row to the domain entity. */
private fun Transactions.toEntity() = HttpTransactionEntity(
    id = id,
    startedAt = startedAt,
    completedAt = completedAt,
    tookMs = tookMs,
    method = method,
    url = url,
    host = host,
    path = path,
    scheme = scheme,
    protocol = protocol,
    requestHeaders = requestHeaders,
    requestContentType = requestContentType,
    requestContentLength = requestContentLength,
    requestBody = requestBody,
    requestBodyKind = requestBodyKind,
    statusCode = statusCode?.toInt(),
    statusMessage = statusMessage,
    responseHeaders = responseHeaders,
    responseContentType = responseContentType,
    responseContentLength = responseContentLength,
    responseBody = responseBody,
    responseBodyKind = responseBodyKind,
    responseImageBytes = responseImageBytes,
    error = error,
)
