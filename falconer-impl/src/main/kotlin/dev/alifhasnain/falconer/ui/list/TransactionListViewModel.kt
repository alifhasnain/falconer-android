package dev.alifhasnain.falconer.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.alifhasnain.falconer.data.HttpTransactionDao
import dev.alifhasnain.falconer.data.HttpTransactionEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * List screen state.
 *
 * [Loading] is the state *before the first row set has ever arrived* — it is not
 * "no rows". Keeping the two apart is what lets the UI show a skeleton instead of
 * flashing "No transactions captured yet" at a database that simply has not
 * answered yet (PRODUCT principle 3: never claim something the data does not say).
 */
sealed interface TransactionListState {
    data object Loading : TransactionListState
    data class Ready(val items: List<HttpTransactionEntity>) : TransactionListState
}

/** List screen: live transactions, filtered by the search query, plus clear. */
class TransactionListViewModel(
    private val dao: HttpTransactionDao,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Emits [TransactionListState.Loading] until the DAO's first emission, then
     * [TransactionListState.Ready] forever after.
     *
     * A query change swaps the upstream via `flatMapLatest`, but the downstream
     * `StateFlow` holds its last value until the new upstream emits — so typing
     * never drops the screen back to Loading.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TransactionListState> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) dao.observeAll() else dao.observeFiltered("%${q.trim()}%")
        }
        .map<List<HttpTransactionEntity>, TransactionListState> { TransactionListState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionListState.Loading)

    fun setQuery(value: String) {
        _query.value = value
    }

    fun clear() {
        viewModelScope.launch { dao.clear() }
    }
}
