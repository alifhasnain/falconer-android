package dev.alifhasnain.falconer.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alifhasnain.falconer.data.HttpTransactionDao
import dev.alifhasnain.falconer.data.HttpTransactionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Detail screen state. [Gone] is "the database answered: no such row" — the
 * transaction was cleared (host app, notification, list) or swept by retention.
 * Kept apart from [Loading] so the screen can leave instead of spinning forever.
 */
sealed interface TransactionDetailState {
    data object Loading : TransactionDetailState
    data class Ready(val transaction: HttpTransactionEntity) : TransactionDetailState
    data object Gone : TransactionDetailState
}

/** Detail screen: the single transaction, observed live by id. */
class TransactionDetailViewModel(
    dao: HttpTransactionDao,
    id: String,
) : ViewModel() {

    // observeById re-runs on every write to the table; distinctUntilChanged keeps
    // unrelated captures from re-rendering (and re-formatting) this screen.
    val state: StateFlow<TransactionDetailState> = dao.observeById(id)
        .map { tx -> if (tx == null) TransactionDetailState.Gone else TransactionDetailState.Ready(tx) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionDetailState.Loading)
}
