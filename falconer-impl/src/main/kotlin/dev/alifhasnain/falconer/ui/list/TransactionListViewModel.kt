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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** List screen: live transactions, filtered by the search query, plus clear. */
class TransactionListViewModel(
    private val dao: HttpTransactionDao,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<HttpTransactionEntity>> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) dao.observeAll() else dao.observeFiltered("%${q.trim()}%")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun clear() {
        viewModelScope.launch { dao.clear() }
    }
}
