package dev.alifhasnain.falconer.ui.detail.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.alifhasnain.falconer.data.HttpTransactionEntity
import dev.alifhasnain.falconer.ui.common.KeyValueRow
import dev.alifhasnain.falconer.ui.common.SectionHeader
import dev.alifhasnain.falconer.ui.common.Format
import dev.alifhasnain.falconer.ui.theme.FalconerColors

@Composable
fun OverviewTab(tx: HttpTransactionEntity, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        SectionHeader("Request")
        KeyValueRow("Method", tx.method)
        KeyValueRow("URL", tx.url, mono = true)
        KeyValueRow("Host", tx.host, mono = true)
        KeyValueRow("Scheme", tx.scheme)
        KeyValueRow("Started", Format.time(tx.startedAt))

        HorizontalDivider(color = FalconerColors.Outline, modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader("Response")
        KeyValueRow(
            "Status",
            statusLine(tx),
        )
        KeyValueRow("Duration", Format.duration(tx.tookMs))
        KeyValueRow("Protocol", tx.protocol ?: "—")

        HorizontalDivider(color = FalconerColors.Outline, modifier = Modifier.padding(vertical = 8.dp))

        SectionHeader("Sizes & types")
        KeyValueRow("Req. size", Format.size(tx.requestContentLength))
        KeyValueRow("Req. type", tx.requestContentType ?: "—", mono = true)
        KeyValueRow("Resp. size", Format.size(tx.responseContentLength))
        KeyValueRow("Resp. type", tx.responseContentType ?: "—", mono = true)

        if (tx.error != null) {
            HorizontalDivider(color = FalconerColors.Outline, modifier = Modifier.padding(vertical = 8.dp))
            SectionHeader("Error")
            Text(
                tx.error,
                style = MaterialTheme.typography.bodyMedium,
                color = FalconerColors.Red,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

private fun statusLine(tx: HttpTransactionEntity): String = when {
    tx.statusCode != null -> "${tx.statusCode}${tx.statusMessage?.let { " $it" } ?: ""}"
    tx.error != null -> "Failed"
    else -> "In flight"
}
