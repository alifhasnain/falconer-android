package dev.alifhasnain.falconer.ui

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.alifhasnain.falconer.data.FalconerDb
import dev.alifhasnain.falconer.data.HttpTransactionDao
import dev.alifhasnain.falconer.data.SqlDelightHttpTransactionDao
import dev.alifhasnain.falconer.data.testEntity
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

        rule.setContent { FalconerTheme { TransactionListScreen(vm, onOpen = {}) } }

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

    @Test
    fun bodySection_marksEmptyBody() {
        rule.setContent {
            FalconerTheme {
                BodySection(body = null, bodyKind = "none", contentType = null, imageBytes = null)
            }
        }
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
        rule.onNodeWithText("hello", substring = true).assertIsDisplayed()
    }
}
