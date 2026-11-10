package dev.alifhasnain.falconer.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [HttpTransactionDao] for JVM repository tests (no Room runtime). */
class FakeTransactionDao : HttpTransactionDao {
    private val rows = LinkedHashMap<String, HttpTransactionEntity>()
    private val all = MutableStateFlow<List<HttpTransactionEntity>>(emptyList())
    val deleteBeforeCalls = mutableListOf<Long>()

    private fun publish() {
        all.value = rows.values.sortedByDescending { it.startedAt }
    }

    override suspend fun insert(transaction: HttpTransactionEntity) {
        rows[transaction.id] = transaction
        publish()
    }

    override suspend fun update(transaction: HttpTransactionEntity) {
        rows[transaction.id] = transaction
        publish()
    }

    override suspend fun findById(id: String): HttpTransactionEntity? = rows[id]

    /** Full rows — the fake's own view, used by tests that assert persistence. */
    fun observeAll(): Flow<List<HttpTransactionEntity>> = all

    override fun observeListRows(): Flow<List<TransactionListRow>> =
        all.map { list -> list.map(HttpTransactionEntity::toListRow) }

    override fun observeListRowsFiltered(query: String): Flow<List<TransactionListRow>> {
        val needle = query.trim('%')
        return all.map { list ->
            list.filter {
                it.url.contains(needle) ||
                    (it.requestBody?.contains(needle) ?: false) ||
                    (it.responseBody?.contains(needle) ?: false)
            }.map(HttpTransactionEntity::toListRow)
        }
    }

    override fun observeRecent(limit: Long): Flow<List<TransactionListRow>> =
        all.map { list -> list.take(limit.toInt()).map(HttpTransactionEntity::toListRow) }

    override fun observeById(id: String): Flow<HttpTransactionEntity?> =
        all.map { list -> list.firstOrNull { it.id == id } }

    override fun observeCount(): Flow<Int> = all.map { it.size }

    override suspend fun clear() {
        rows.clear()
        publish()
    }

    override suspend fun deleteBefore(threshold: Long) {
        deleteBeforeCalls.add(threshold)
        rows.values.removeAll { it.startedAt < threshold }
        publish()
    }
}

/** The projection SQLDelight does in SQL, done in Kotlin for the fake. */
internal fun HttpTransactionEntity.toListRow() = TransactionListRow(
    id = id,
    startedAt = startedAt,
    tookMs = tookMs,
    method = method,
    host = host,
    path = path,
    statusCode = statusCode,
    responseContentLength = responseContentLength,
    error = error,
)
