package dev.alifhasnain.falconer.ui.list

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.alifhasnain.falconer.data.TransactionListRow
import dev.alifhasnain.falconer.ui.common.Format
import dev.alifhasnain.falconer.ui.common.MethodChip
import dev.alifhasnain.falconer.ui.common.StatusText
import dev.alifhasnain.falconer.ui.theme.FalconerColors
import dev.alifhasnain.falconer.ui.theme.MonoStyle
import kotlinx.coroutines.delay

/**
 * How long the screen may stay loading before the skeleton appears. Under this,
 * the data wins the race and no placeholder is ever drawn — a one-frame flash of
 * shimmer is noise, not polish.
 */
private const val SKELETON_GRACE_MS = 120L

/** Once shown, the skeleton holds this long so it reads as a state, not a blink. */
private const val SKELETON_MIN_HOLD_MS = 320L

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel,
    onOpen: (String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }

    val items = (state as? TransactionListState.Ready)?.items.orEmpty()
    val showSkeleton = rememberSkeletonVisible(state is TransactionListState.Loading)

    Scaffold(
        containerColor = FalconerColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Falconer", style = MaterialTheme.typography.titleMedium) },
                actions = {
                    IconButton(onClick = { confirmClear = true }, enabled = items.isNotEmpty()) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear all")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FalconerColors.Surface),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                textStyle = MonoStyle,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                placeholder = { Text("Search url, request, response", style = MonoStyle) },
            )
            HorizontalDivider(color = FalconerColors.Outline)

            Crossfade(
                targetState = showSkeleton,
                animationSpec = tween(durationMillis = 180),
                label = "listContent",
            ) { skeleton ->
                when {
                    skeleton -> TransactionListSkeleton()
                    items.isEmpty() -> EmptyState(searching = query.isNotBlank())
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        items(items, key = { it.id }) { tx ->
                            TransactionRow(tx, onClick = { onOpen(tx.id) })
                            HorizontalDivider(color = FalconerColors.Outline)
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all transactions?") },
            text = { Text("This permanently deletes every captured transaction on this device.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clear(); confirmClear = false }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }
}

/**
 * Debounces [loading] into skeleton visibility: nothing for the first
 * [SKELETON_GRACE_MS] (fast loads never flash), and once shown it holds for at
 * least [SKELETON_MIN_HOLD_MS] so the transition is legible rather than a stutter.
 *
 * Cancellation of the previous [LaunchedEffect] does the work — if loading ends
 * inside the grace window the pending "show" coroutine is cancelled before it fires.
 */
@Composable
private fun rememberSkeletonVisible(loading: Boolean): Boolean {
    var visible by remember { mutableStateOf(false) }
    var shownAt by remember { mutableStateOf(0L) }

    LaunchedEffect(loading) {
        if (loading) {
            delay(SKELETON_GRACE_MS)
            shownAt = SystemClock.uptimeMillis()
            visible = true
        } else if (visible) {
            val elapsed = SystemClock.uptimeMillis() - shownAt
            if (elapsed < SKELETON_MIN_HOLD_MS) delay(SKELETON_MIN_HOLD_MS - elapsed)
            visible = false
        }
    }
    return visible
}

@Composable
private fun TransactionRow(tx: TransactionListRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MethodChip(tx.method)
                Spacer(Modifier.width(8.dp))
                Text(
                    tx.path.ifBlank { "/" },
                    style = MonoStyle.copy(fontWeight = FontWeight.Medium),
                    color = FalconerColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(0.dp))
            Text(
                tx.host,
                style = MaterialTheme.typography.labelSmall,
                color = FalconerColors.InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            StatusText(tx.statusCode, tx.error)
            Text(
                buildString {
                    append(Format.duration(tx.tookMs))
                    append("  ·  ")
                    append(Format.size(tx.responseContentLength))
                },
                style = MaterialTheme.typography.labelSmall,
                color = FalconerColors.InkSecondary,
            )
        }
    }
}

@Composable
private fun EmptyState(searching: Boolean) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (searching) "No matching transactions" else "No transactions captured yet",
            style = MaterialTheme.typography.titleMedium,
            color = FalconerColors.InkSecondary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (searching) "Try a different search." else "Fire a request from your app to see it here.",
            style = MaterialTheme.typography.bodyMedium,
            color = FalconerColors.InkSecondary,
        )
    }
}
