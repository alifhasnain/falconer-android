package dev.alifhasnain.falconer.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import dev.alifhasnain.falconer.data.HttpTransactionEntity
import dev.alifhasnain.falconer.export.CurlBuilder
import dev.alifhasnain.falconer.export.ShareDelegate
import dev.alifhasnain.falconer.export.TextExporter
import dev.alifhasnain.falconer.ui.common.MethodChip
import dev.alifhasnain.falconer.ui.common.MutedNote
import dev.alifhasnain.falconer.ui.detail.tabs.OverviewTab
import dev.alifhasnain.falconer.ui.detail.tabs.RequestTab
import dev.alifhasnain.falconer.ui.detail.tabs.ResponseTab
import dev.alifhasnain.falconer.ui.theme.FalconerColors

private val TABS = listOf("Overview", "Request", "Response")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    viewModel: TransactionDetailViewModel,
    onBack: () -> Unit,
) {
    val tx by viewModel.transaction.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { TABS.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = FalconerColors.Background,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    val current = tx
                    if (current == null) {
                        Text("Transaction")
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MethodChip(current.method)
                            Text(
                                current.path,
                                style = MaterialTheme.typography.titleMedium,
                                color = FalconerColors.Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                },
                actions = { ShareMenu(tx) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FalconerColors.Surface),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val current = tx
            if (current == null) {
                MutedNote("Loading…")
                return@Column
            }

            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = FalconerColors.Surface,
            ) {
                TABS.forEachIndexed { index, label ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(label, style = MaterialTheme.typography.labelLarge) },
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> OverviewTab(current, Modifier.fillMaxSize())
                    1 -> RequestTab(current, Modifier.fillMaxSize())
                    else -> ResponseTab(current, Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun ShareMenu(tx: HttpTransactionEntity?) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }

    IconButton(onClick = { open = true }, enabled = tx != null) {
        Icon(Icons.Default.Share, contentDescription = "Share")
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text("Share as cURL") },
            onClick = {
                open = false
                tx?.let { ShareDelegate.shareText(context, CurlBuilder.build(it), "Falconer cURL") }
            },
        )
        DropdownMenuItem(
            text = { Text("Share as text") },
            onClick = {
                open = false
                tx?.let { ShareDelegate.shareText(context, TextExporter.build(it), "Falconer export") }
            },
        )
        DropdownMenuItem(
            text = { Text("Save as .txt") },
            onClick = {
                open = false
                tx?.let { ShareDelegate.shareTextFile(context, fileNameFor(it), TextExporter.build(it)) }
            },
        )
    }
}

private fun fileNameFor(tx: HttpTransactionEntity): String {
    val safePath = tx.path.trim('/')
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .ifBlank { "root" }
    return "falconer-${tx.method}-$safePath.txt"
}
