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

    override fun observeAll(): Flow<List<HttpTransactionEntity>> = all

    override fun observeFiltered(query: String): Flow<List<HttpTransactionEntity>> {
        val needle = query.trim('%')
        return all.map { list ->
            list.filter {
                it.url.contains(needle) ||
                    (it.requestBody?.contains(needle) ?: false) ||
                    (it.responseBody?.contains(needle) ?: false)
            }
        }
    }

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
