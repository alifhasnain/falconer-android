package dev.alifhasnain.falconer.ui.detail.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.alifhasnain.falconer.data.Converters
import dev.alifhasnain.falconer.data.HttpTransactionEntity
import dev.alifhasnain.falconer.ui.common.CollapsibleSectionHeader
import dev.alifhasnain.falconer.ui.common.HeadersBlock
import dev.alifhasnain.falconer.ui.common.SectionHeader
import dev.alifhasnain.falconer.ui.detail.BodySection
import dev.alifhasnain.falconer.ui.theme.FalconerColors

@Composable
fun ResponseTab(tx: HttpTransactionEntity, modifier: Modifier = Modifier) {
    // Transport failure with no response: show the error, not an empty shell.
    if (tx.statusCode == null && tx.error != null) {
        Column(modifier.fillMaxSize()) {
            SectionHeader("Error")
            Text(
                tx.error,
                style = MaterialTheme.typography.bodyMedium,
                color = FalconerColors.Red,
                modifier = Modifier.padding(16.dp),
            )
        }
        return
    }

    // Collapsed by default: the body is the product — let it own the viewport.
    var headersExpanded by remember { mutableStateOf(false) }
    val headerCount = remember(tx.responseHeaders) {
        Converters.jsonToHeaders(tx.responseHeaders).size
    }

    Column(modifier.fillMaxSize()) {
        CollapsibleSectionHeader(
            title = "Headers",
            expanded = headersExpanded,
            onToggle = { headersExpanded = !headersExpanded },
            count = headerCount,
        )
        if (headersExpanded) {
            Box(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                HeadersBlock(tx.responseHeaders)
            }
        }
        HorizontalDivider(color = FalconerColors.Outline)
        SectionHeader("Body")
        BodySection(
            body = tx.responseBody,
            bodyKind = tx.responseBodyKind,
            contentType = tx.responseContentType,
            imageBytes = tx.responseImageBytes,
            modifier = Modifier.weight(1f),
        )
    }
}
