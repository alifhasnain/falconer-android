package dev.alifhasnain.falconer.ui.detail

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import dev.alifhasnain.falconer.ui.common.BodySearch
import dev.alifhasnain.falconer.ui.common.JsonFormatter
import dev.alifhasnain.falconer.ui.common.MutedNote
import dev.alifhasnain.falconer.ui.theme.FalconerColors
import dev.alifhasnain.falconer.ui.theme.MonoStyle

/**
 * Renders a captured body: an image preview on the image path, otherwise the
 * (optionally pretty-printed) text with in-body search + match highlighting and
 * prev/next navigation. Empty bodies and markers are shown, never hidden.
 */
@Composable
fun BodySection(
    body: String?,
    bodyKind: String?,
    contentType: String?,
    imageBytes: ByteArray?,
    modifier: Modifier = Modifier,
) {
    if (imageBytes != null) {
        ImagePreview(imageBytes, modifier)
        return
    }

    val text = remember(body, bodyKind) {
        if (bodyKind == "json") JsonFormatter.format(body) else (body ?: "")
    }

    if (text.isBlank()) {
        MutedNote("(no body)", modifier)
        return
    }

    val caption = buildString {
        append(bodyKind ?: "body")
        if (!contentType.isNullOrBlank()) append("  ·  ").also { append(contentType) }
    }
    SearchableBody(text, caption, modifier)
}

@Composable
private fun SearchableBody(text: String, caption: String, modifier: Modifier) {
    var query by remember(text) { mutableStateOf("") }
    var current by remember(text) { mutableStateOf(0) }
    val matches = remember(text, query) { BodySearch.findMatches(text, query) }
    val scroll = rememberScrollState()
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }

    val matchStyle = SpanStyle(background = FalconerColors.Amber.copy(alpha = 0.30f))
    val currentStyle = SpanStyle(background = FalconerColors.Amber, color = Color.White)

    LaunchedEffect(matches) { current = 0 }
    LaunchedEffect(current, matches, layout) {
        val result = layout ?: return@LaunchedEffect
        if (matches.isEmpty()) return@LaunchedEffect
        val index = current.coerceIn(0, matches.size - 1)
        val top = result.getBoundingBox(matches[index].first).top.toInt()
        scroll.animateScrollTo((top - 48).coerceAtLeast(0))
    }

    Column(modifier.fillMaxSize()) {
        Text(
            caption,
            style = MaterialTheme.typography.labelSmall,
            color = FalconerColors.InkSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        InBodySearchBar(
            query = query,
            onQuery = { query = it },
            matchCount = matches.size,
            current = current,
            onPrev = { if (matches.isNotEmpty()) current = (current - 1 + matches.size) % matches.size },
            onNext = { if (matches.isNotEmpty()) current = (current + 1) % matches.size },
        )
        HorizontalDivider(color = FalconerColors.Outline)
        SelectionContainer(Modifier.weight(1f)) {
            Text(
                text = BodySearch.highlight(text, matches, current, matchStyle, currentStyle),
                style = MonoStyle,
                color = FalconerColors.Ink,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(16.dp),
                onTextLayout = { layout = it },
            )
        }
    }
}

@Composable
private fun InBodySearchBar(
    query: String,
    onQuery: (String) -> Unit,
    matchCount: Int,
    current: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Compact field: a slim bordered pill, not the 56dp Material default —
        // the body is the product, the find-bar recedes (PRODUCT principles 1, 5).
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = MonoStyle.copy(color = FalconerColors.Ink),
            cursorBrush = SolidColor(FalconerColors.Ink),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 36.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, FalconerColors.Outline, RoundedCornerShape(8.dp)),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = FalconerColors.InkSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                "Find in body",
                                style = MonoStyle,
                                color = FalconerColors.InkSecondary,
                            )
                        }
                        inner()
                    }
                }
            },
        )
        Text(
            text = if (matchCount == 0) "0/0" else "${current + 1}/$matchCount",
            style = MaterialTheme.typography.labelSmall,
            color = FalconerColors.InkSecondary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        IconButton(
            onClick = onPrev,
            enabled = matchCount > 0,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                Icons.Default.KeyboardArrowUp,
                contentDescription = "Previous match",
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
            onClick = onNext,
            enabled = matchCount > 0,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Next match",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ImagePreview(bytes: ByteArray, modifier: Modifier) {
    // D8: plain BitmapFactory -> asImageBitmap, no Coil dependency.
    val bitmap = remember(bytes) {
        runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull()
    }
    if (bitmap == null) {
        MutedNote("[image could not be decoded]", modifier)
        return
    }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            "${bytes.size} bytes",
            style = MaterialTheme.typography.labelSmall,
            color = FalconerColors.InkSecondary,
        )
        Spacer(Modifier.height(8.dp))
        Image(
            bitmap = bitmap,
            contentDescription = "Response image",
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
        )
    }
}
