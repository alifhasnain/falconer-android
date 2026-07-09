package dev.alifhasnain.falconer.ui.detail.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.alifhasnain.falconer.data.HttpTransactionEntity
import dev.alifhasnain.falconer.ui.common.HeadersBlock
import dev.alifhasnain.falconer.ui.common.SectionHeader
import dev.alifhasnain.falconer.ui.detail.BodySection
import dev.alifhasnain.falconer.ui.theme.FalconerColors

@Composable
fun RequestTab(tx: HttpTransactionEntity, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        SectionHeader("Headers")
        Box(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
            HeadersBlock(tx.requestHeaders)
        }
        HorizontalDivider(color = FalconerColors.Outline)
        SectionHeader("Body")
        BodySection(
            body = tx.requestBody,
            bodyKind = tx.requestBodyKind,
            contentType = tx.requestContentType,
            imageBytes = null,
            modifier = Modifier.weight(1f),
        )
    }
}
