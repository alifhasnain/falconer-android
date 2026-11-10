package dev.alifhasnain.falconer.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.alifhasnain.falconer.data.FalconerDatabase
import dev.alifhasnain.falconer.ui.detail.TransactionDetailScreen
import dev.alifhasnain.falconer.ui.detail.TransactionDetailViewModel
import dev.alifhasnain.falconer.ui.list.TransactionListScreen
import dev.alifhasnain.falconer.ui.list.TransactionListViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Navigation root: list -> detail/{id}. Each [reopened] emission (the running UI
 * re-launched from the notification or the host app) returns to the top of the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FalconerApp(reopened: Flow<Unit> = emptyFlow()) {
    val context = LocalContext.current
    val dao = remember { FalconerDatabase.get(context).transactionDao() }
    val navController = rememberNavController()
    // Hoisted above the NavHost so the reopen handler can reach it even while a
    // detail screen is on top.
    val listState = rememberLazyListState()

    LaunchedEffect(reopened) {
        reopened.collect {
            navController.popBackStack("list", inclusive = false)
            listState.scrollToItem(0)
        }
    }

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            val vm: TransactionListViewModel =
                viewModel(factory = FalconerViewModelFactory(dao))
            TransactionListScreen(
                viewModel = vm,
                onOpen = { id -> navController.navigate("detail/$id") },
                listState = listState,
            )
        }
        composable(
            route = "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val vm: TransactionDetailViewModel =
                viewModel(key = id, factory = FalconerViewModelFactory(dao, id))
            TransactionDetailScreen(
                viewModel = vm,
                // Pop *to* the list, never past it: a deleted row's auto-back and a
                // reopen can both fire, and a second plain pop would empty the stack.
                onBack = { navController.popBackStack("list", inclusive = false) },
            )
        }
    }
}
