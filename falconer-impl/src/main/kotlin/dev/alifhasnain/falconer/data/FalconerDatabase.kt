package dev.alifhasnain.falconer.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * The on-device store for captured transactions.
 *
 * Wraps the SQLDelight-generated [FalconerDb] and exposes a single
 * [HttpTransactionDao] so callers (plugin, UI, receivers) stay decoupled from
 * generated code. v1 is a single-version, single-table database; SQLDelight
 * verifies the schema and any future `.sqm` migrations at build time.
 */
class FalconerDatabase private constructor(driver: SqlDriver) {

    private val dao: HttpTransactionDao =
        SqlDelightHttpTransactionDao(FalconerDb(driver).transactionsQueries)

    fun transactionDao(): HttpTransactionDao = dao

    companion object {
        @Volatile
        private var instance: FalconerDatabase? = null

        fun get(context: Context): FalconerDatabase =
            instance ?: synchronized(this) {
                instance ?: FalconerDatabase(
                    AndroidSqliteDriver(
                        schema = FalconerDb.Schema,
                        context = context.applicationContext,
                        name = "falconer.db",
                    ),
                ).also { instance = it }
            }
    }
}
