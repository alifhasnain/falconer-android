package dev.alifhasnain.falconer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.alifhasnain.falconer.data.HttpTransactionDao
import dev.alifhasnain.falconer.ui.detail.TransactionDetailViewModel
import dev.alifhasnain.falconer.ui.list.TransactionListViewModel

/** Builds the screen ViewModels with the DAO (and detail id) injected. */
class FalconerViewModelFactory(
    private val dao: HttpTransactionDao,
    private val detailId: String? = null,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(TransactionListViewModel::class.java) ->
            TransactionListViewModel(dao) as T

        modelClass.isAssignableFrom(TransactionDetailViewModel::class.java) ->
            TransactionDetailViewModel(dao, requireNotNull(detailId)) as T

        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
