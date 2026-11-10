package dev.alifhasnain.falconer.ui.detail

import dev.alifhasnain.falconer.data.FakeTransactionDao
import dev.alifhasnain.falconer.data.HttpTransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * A deleted row must resolve to Gone, never sit at Loading — the screen pops back
 * to the list on Gone (the "stuck at Loading after clear" bug).
 */
internal class TransactionDetailViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun existingRowIsReady() = runTest {
        val dao = FakeTransactionDao()
        dao.insert(entity("a"))
        val vm = TransactionDetailViewModel(dao, "a")

        val state = vm.state.first { it !is TransactionDetailState.Loading }

        assertIs<TransactionDetailState.Ready>(state)
        assertEquals("a", state.transaction.id)
    }

    @Test
    fun clearedRowBecomesGone() = runTest {
        val dao = FakeTransactionDao()
        dao.insert(entity("a"))
        val vm = TransactionDetailViewModel(dao, "a")
        vm.state.first { it is TransactionDetailState.Ready }

        dao.clear()

        assertIs<TransactionDetailState.Gone>(vm.state.first { it !is TransactionDetailState.Ready })
    }

    private fun entity(id: String) = HttpTransactionEntity(
        id = id,
        startedAt = 0,
        completedAt = null,
        tookMs = null,
        method = "GET",
        url = "https://x/y",
        host = "x",
        path = "/y",
        scheme = "https",
        protocol = null,
        requestHeaders = "{}",
        requestContentType = null,
        requestContentLength = null,
        requestBody = null,
        requestBodyKind = "none",
        statusCode = 200,
        statusMessage = "OK",
        responseHeaders = null,
        responseContentType = null,
        responseContentLength = null,
        responseBody = null,
        responseBodyKind = null,
        responseImageBytes = null,
        error = null,
    )
}
