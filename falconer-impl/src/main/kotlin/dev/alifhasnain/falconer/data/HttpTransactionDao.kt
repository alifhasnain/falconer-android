package dev.alifhasnain.falconer.data

import kotlinx.coroutines.flow.Flow

/**
 * Persistence contract for captured transactions. Kept as a hand-written
 * interface (not generated) so the repository, view-models and tests depend on
 * a stable seam; [SqlDelightHttpTransactionDao] is the production backing and
 * [FakeTransactionDao] the test one. The SQL lives in `Transactions.sq`.
 */
interface HttpTransactionDao {

    /** Inserts, replacing any row with the same id (the request-insert path). */
    suspend fun insert(transaction: HttpTransactionEntity)

    /** Full-row replace by id — the response/error merge path. */
    suspend fun update(transaction: HttpTransactionEntity)

    /** One-shot read used by the insert-then-update merge path. */
    suspend fun findById(id: String): HttpTransactionEntity?

    fun observeAll(): Flow<List<HttpTransactionEntity>>

    fun observeFiltered(query: String): Flow<List<HttpTransactionEntity>>

    fun observeById(id: String): Flow<HttpTransactionEntity?>

    fun observeCount(): Flow<Int>

    suspend fun clear()

    suspend fun deleteBefore(threshold: Long)
}
