package dev.alifhasnain.falconer.ui

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.alifhasnain.falconer.data.FalconerDb
import dev.alifhasnain.falconer.data.HttpTransactionDao
import dev.alifhasnain.falconer.data.SqlDelightHttpTransactionDao
import dev.alifhasnain.falconer.data.testEntity
import dev.alifhasnain.falconer.ui.common.LocalShimmerAnimationEnabled
import dev.alifhasnain.falconer.ui.detail.BodySection
import dev.alifhasnain.falconer.ui.list.TransactionListScreen
import dev.alifhasnain.falconer.ui.list.TransactionListViewModel
import dev.alifhasnain.falconer.ui.theme.FalconerTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class ComposeUiTest {

    @get:Rule
    val rule = createComposeRule()

    // In-memory SQLite (name = null) on the device, behind the production DAO.
    private fun inMemoryDao(): HttpTransactionDao {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val driver = AndroidSqliteDriver(FalconerDb.Schema, context.applicationContext, name = null)
        return SqlDelightHttpTransactionDao(FalconerDb(driver).transactionsQueries)
    }

    @Test
    fun list_rendersSeededTransactionAndFilters() {
        val dao = inMemoryDao()
        runBlocking {
            dao.insert(testEntity("a", path = "/users", url = "https://x/users"))
            dao.insert(testEntity("b", path = "/posts", url = "https://x/posts"))
        }
        val vm = TransactionListViewModel(dao)

        // The loading skeleton's sweep is an infinite animation — under the test
        // clock it would keep the idling resource busy forever, exactly like the
        // blinking cursor noted below. Render the placeholders static instead.
        rule.setContent {
            CompositionLocalProvider(LocalShimmerAnimationEnabled provides false) {
                FalconerTheme { TransactionListScreen(vm, onOpen = {}) }
            }
        }

        rule.onNodeWithText("/users").assertIsDisplayed()
        rule.onNodeWithText("/posts").assertIsDisplayed()

        // Drive the filter through the VM instead of typing into the field. Focusing the
        // OutlinedTextField starts the blinking-cursor animation — an infinite animation
        // that makes waitForIdle()/auto-sync chase frames forever under the test clock,
        // hanging the test until CI cancels it. setQuery exercises the same filter path.
        vm.setQuery("posts")
        rule.waitForIdle()
        rule.onNodeWithText("/posts").assertIsDisplayed()
        rule.onNodeWithText("/users").assertDoesNotExist()
    }

    // Lists here are longer than the screen on purpose: a list that fits on screen
    // is always drawn from the top, which would hide the anchoring being tested.
    private fun seedRows(dao: HttpTransactionDao, prefix: String, startedAt: IntRange) = runBlocking {
        startedAt.forEach { dao.insert(testEntity("$prefix$it", startedAt = it.toLong(), path = "/$prefix$it")) }
    }

    @Test
    fun list_followsNewestRowWhileAtTop() {
        val dao = inMemoryDao()
        seedRows(dao, "row", 1..40)
        val vm = TransactionListViewModel(dao)
        setList(vm, LazyListState())
        awaitText("/row40")

        // Without following, the list stays anchored on "/row40" and the new row
        // lands off-screen above it.
        runBlocking { dao.insert(testEntity("n", startedAt = 100, path = "/newest")) }
        awaitText("/newest")
        rule.onNodeWithText("/newest").assertIsDisplayed()
    }

    @Test
    fun list_followsNewestAgainAfterClearWhileScrolledDown() {
        val dao = inMemoryDao()
        seedRows(dao, "row", 1..40)
        val vm = TransactionListViewModel(dao)
        setList(vm, LazyListState())
        awaitText("/row40")

        // Scrolled down: following is off. A clear must turn it back on.
        rule.onNode(hasScrollToIndexAction()).performScrollToIndex(20)
        rule.waitForIdle()
        vm.clear()
        awaitText("No transactions captured yet")

        seedRows(dao, "new", 100..139)
        awaitText("/new139")
        rule.onNodeWithText("/new139").assertIsDisplayed()
    }

    private fun setList(vm: TransactionListViewModel, listState: LazyListState) {
        rule.setContent {
            CompositionLocalProvider(LocalShimmerAnimationEnabled provides false) {
                FalconerTheme { TransactionListScreen(vm, onOpen = {}, listState = listState) }
            }
        }
    }

    // DB flows and body formatting finish off the main thread, which waitForIdle
    // does not track — poll for the text instead.
    private fun awaitText(text: String, substring: Boolean = false) {
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun bodySection_marksEmptyBody() {
        rule.setContent {
            FalconerTheme {
                BodySection(body = null, bodyKind = "none", contentType = null, imageBytes = null)
            }
        }
        awaitText("(no body)")
        rule.onNodeWithText("(no body)").assertIsDisplayed()
    }

    @Test
    fun bodySection_showsJsonText() {
        rule.setContent {
            FalconerTheme {
                BodySection(
                    body = "{\"hello\":\"world\"}",
                    bodyKind = "json",
                    contentType = "application/json",
                    imageBytes = null,
                )
            }
        }
        awaitText("hello", substring = true)
        rule.onNodeWithText("hello", substring = true).assertIsDisplayed()
    }
}
