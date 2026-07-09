package dev.alifhasnain.falconer.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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

/** Navigation root: list -> detail/{id}. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FalconerApp() {
    val context = LocalContext.current
    val dao = remember { FalconerDatabase.get(context).transactionDao() }
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            val vm: TransactionListViewModel =
                viewModel(factory = FalconerViewModelFactory(dao))
            TransactionListScreen(
                viewModel = vm,
                onOpen = { id -> navController.navigate("detail/$id") },
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
                onBack = { navController.popBackStack() },
            )
        }
    }
}
