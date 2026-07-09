package dev.alifhasnain.falconer.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alifhasnain.falconer.data.HttpTransactionDao
import dev.alifhasnain.falconer.data.HttpTransactionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Detail screen: the single transaction, observed live by id. */
class TransactionDetailViewModel(
    dao: HttpTransactionDao,
    id: String,
) : ViewModel() {

    val transaction: StateFlow<HttpTransactionEntity?> = dao.observeById(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
