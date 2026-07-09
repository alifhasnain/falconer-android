package dev.alifhasnain.falconer.data

import dev.alifhasnain.falconer.channel.ErrorPayload
import dev.alifhasnain.falconer.channel.RequestPayload
import dev.alifhasnain.falconer.channel.ResponsePayload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class TransactionRepositoryTest {

    private class MemStore(var value: Long = 0L) : LastCleanupStore {
        override fun get(): Long = value
        override fun set(value: Long) {
            this.value = value
        }
    }

    private fun repo(dao: FakeTransactionDao): TransactionRepository {
        // now()=0 + FOREVER keeps retention from deleting anything mid-test.
        val retention = RetentionManager(
            lastCleanup = MemStore(),
            deleteBefore = dao::deleteBefore,
            now = { 0L },
        ).apply { period = RetentionPeriod.FOREVER }
        return TransactionRepository(dao, retention)
    }

    private fun request(id: String, startedAt: Long = 100L) = RequestPayload(
        id = id,
        startedAt = startedAt,
        method = "GET",
        url = "https://api.example.com/users",
        host = "api.example.com",
        path = "/users",
        scheme = "https",
        requestHeaders = mapOf("accept" to "application/json"),
        requestContentType = null,
        requestContentLength = null,
        requestBody = null,
        requestBodyKind = "none",
    )

    @Test
    fun insertThenResponse_mergesIntoOneRow() = runTest {
        val dao = FakeTransactionDao()
        val r = repo(dao)

        r.logRequest(request("t1"))
        r.logResponse(
            ResponsePayload(
                id = "t1",
                completedAt = 250L,
                tookMs = 150L,
                statusCode = 200,
                statusMessage = "OK",
                protocol = null,
                responseHeaders = mapOf("content-type" to "application/json"),
                responseContentType = "application/json",
                responseContentLength = 11L,
                responseBody = "{\"ok\":true}",
                responseBodyKind = "json",
                responseImageBytes = null,
            ),
        )

        assertEquals(1, dao.observeAll().first().size)
        val row = dao.findById("t1")!!
        assertEquals("GET", row.method)
        assertEquals(200, row.statusCode)
        assertEquals(150L, row.tookMs)
        assertEquals("{\"ok\":true}", row.responseBody)
        assertEquals(mapOf("accept" to "application/json"), Converters.jsonToHeaders(row.requestHeaders))
        assertNull(row.error)
    }

    @Test
    fun responseWithoutRequest_createsRow() = runTest {
        val dao = FakeTransactionDao()
        val r = repo(dao)

        r.logResponse(
            ResponsePayload(
                id = "orphan",
                completedAt = 5L,
                tookMs = 1L,
                statusCode = 404,
                statusMessage = "Not Found",
                protocol = null,
                responseHeaders = emptyMap(),
                responseContentType = null,
                responseContentLength = null,
                responseBody = "missing",
                responseBodyKind = "text",
                responseImageBytes = null,
            ),
        )

        val row = dao.findById("orphan")!!
        assertEquals(404, row.statusCode)
        assertEquals("missing", row.responseBody)
    }

    @Test
    fun errorWithoutRequest_createsRow() = runTest {
        val dao = FakeTransactionDao()
        val r = repo(dao)

        r.logError(ErrorPayload(id = "e1", completedAt = 10L, tookMs = 5L, error = "timeout"))

        val row = dao.findById("e1")!!
        assertEquals("timeout", row.error)
    }

    @Test
    fun clear_removesEverything() = runTest {
        val dao = FakeTransactionDao()
        val r = repo(dao)

        r.logRequest(request("a"))
        r.logRequest(request("b"))
        assertEquals(2, dao.observeAll().first().size)

        r.clear()
        assertTrue(dao.observeAll().first().isEmpty())
    }
}
