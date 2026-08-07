package dev.alifhasnain.falconer.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * SQLDelight DAO behavior on the real SQLite engine, in pure JVM (no device —
 * replaces the old Room instrumented test). Covers the insert-then-update merge,
 * deleteBefore staleness, filtered LIKE, count, and on-disk persistence.
 */
internal class HttpTransactionDaoTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var dao: HttpTransactionDao

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FalconerDb.Schema.create(driver)
        dao = SqlDelightHttpTransactionDao(FalconerDb(driver).transactionsQueries)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    @Test
    fun insertThenUpdate_yieldsOneMergedRow() = runTest {
        dao.insert(testEntity("a", startedAt = 100))
        val merged = dao.findById("a")!!.copy(statusCode = 200, responseBody = "ok")
        dao.update(merged)

        val rows = dao.observeListRows().first()
        assertEquals(1, rows.size)
        assertEquals(200, rows[0].statusCode)
        // The body is not in the list projection — read it back through detail.
        assertEquals("ok", dao.findById("a")!!.responseBody)
    }

    @Test
    fun listProjection_carriesOnlyTheColumnsTheListRenders() = runTest {
        dao.insert(
            testEntity(
                "a",
                startedAt = 100,
                url = "https://api.example.com/users",
                path = "/users",
                requestBody = "req",
                responseBody = "res",
            ),
        )

        val row = dao.observeListRows().first().single()

        assertEquals("a", row.id)
        assertEquals(100, row.startedAt)
        assertEquals("GET", row.method)
        assertEquals("/users", row.path)
        // Bodies and the image BLOB are absent by construction: TransactionListRow
        // has no field for them, so a wide read cannot creep back in unnoticed.
    }

    @Test
    fun deleteBefore_removesOnlyStaleRows() = runTest {
        dao.insert(testEntity("old", startedAt = 10))
        dao.insert(testEntity("new", startedAt = 100))

        dao.deleteBefore(50)

        assertEquals(setOf("new"), dao.observeListRows().first().map { it.id }.toSet())
    }

    @Test
    fun observeListRowsFiltered_matchesUrlAndBodies() = runTest {
        dao.insert(testEntity("u", url = "https://x/users", requestBody = "needle-in-request"))
        dao.insert(testEntity("p", url = "https://x/posts", responseBody = "needle-in-response"))
        dao.insert(testEntity("z", url = "https://x/zzz"))

        // Bodies stay searchable even though the projection does not return them.
        assertEquals(
            setOf("u", "p"),
            dao.observeListRowsFiltered("%needle%").first().map { it.id }.toSet(),
        )
        assertEquals(
            setOf("p"),
            dao.observeListRowsFiltered("%posts%").first().map { it.id }.toSet(),
        )
    }

    @Test
    fun observeCount_tracksRowCount() = runTest {
        assertEquals(0, dao.observeCount().first())
        dao.insert(testEntity("a"))
        dao.insert(testEntity("b"))
        assertEquals(2, dao.observeCount().first())
    }

    @Test
    fun transactionsSurviveDatabaseReopen() = runTest {
        val file = File.createTempFile("falconer-reopen", ".db").apply { delete() }
        try {
            val url = "jdbc:sqlite:${file.absolutePath}"

            var fileDriver = JdbcSqliteDriver(url)
            FalconerDb.Schema.create(fileDriver)
            SqlDelightHttpTransactionDao(FalconerDb(fileDriver).transactionsQueries)
                .insert(testEntity("persist"))
            fileDriver.close()

            fileDriver = JdbcSqliteDriver(url)
            val reopened = SqlDelightHttpTransactionDao(FalconerDb(fileDriver).transactionsQueries)
            assertEquals(1, reopened.observeListRows().first().size)
            fileDriver.close()
        } finally {
            file.delete()
        }
    }

    /** Builds a transaction entity for DAO tests. */
    private fun testEntity(
        id: String,
        startedAt: Long = 0,
        method: String = "GET",
        url: String = "https://api.example.com/y",
        path: String = "/y",
        statusCode: Int? = null,
        requestBody: String? = null,
        responseBody: String? = null,
    ) = HttpTransactionEntity(
        id = id,
        startedAt = startedAt,
        completedAt = null,
        tookMs = null,
        method = method,
        url = url,
        host = "api.example.com",
        path = path,
        scheme = "https",
        protocol = null,
        requestHeaders = "{}",
        requestContentType = null,
        requestContentLength = null,
        requestBody = requestBody,
        requestBodyKind = "none",
        statusCode = statusCode,
        statusMessage = null,
        responseHeaders = null,
        responseContentType = null,
        responseContentLength = null,
        responseBody = responseBody,
        responseBodyKind = null,
        responseImageBytes = null,
        error = null,
    )
}
