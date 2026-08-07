package dev.alifhasnain.falconer.ui.list

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
import kotlin.test.assertTrue

/**
 * The list must distinguish "not answered yet" from "answered: nothing" — the
 * loading skeleton hangs off exactly that distinction.
 */
internal class TransactionListViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsLoadingBeforeAnyEmission() {
        val vm = TransactionListViewModel(FakeTransactionDao())
        // Nothing has collected yet, so the upstream is unsubscribed and the
        // StateFlow still holds its initial value.
        assertIs<TransactionListState.Loading>(vm.state.value)
    }

    @Test
    fun emptyDatabaseResolvesToReadyNotLoading() = runTest {
        val vm = TransactionListViewModel(FakeTransactionDao())

        val state = vm.state.first { it is TransactionListState.Ready }

        assertIs<TransactionListState.Ready>(state)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun readyCarriesRowsAndSurvivesQueryChange() = runTest {
        val dao = FakeTransactionDao()
        dao.insert(entity("a", url = "https://x/users"))
        dao.insert(entity("b", url = "https://x/posts"))
        val vm = TransactionListViewModel(dao)

        assertEquals(2, vm.state.first { it is TransactionListState.Ready }.items().size)

        vm.setQuery("posts")
        // Filtering must never drop the screen back to Loading — that would
        // re-show the skeleton on every keystroke.
        assertIs<TransactionListState.Ready>(vm.state.value)

        val filtered = vm.state.first { it is TransactionListState.Ready && it.items.size == 1 }
        assertEquals(listOf("b"), filtered.items().map { it.id })
    }

    private fun TransactionListState.items(): List<HttpTransactionEntity> =
        (this as TransactionListState.Ready).items

    private fun entity(id: String, url: String) = HttpTransactionEntity(
        id = id,
        startedAt = 0,
        completedAt = null,
        tookMs = null,
        method = "GET",
        url = url,
        host = "x",
        path = url.substringAfter("x"),
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
