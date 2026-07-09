package dev.alifhasnain.falconer.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import dev.alifhasnain.falconer.config.FalconerNativeConfig
import dev.alifhasnain.falconer.data.Converters
import dev.alifhasnain.falconer.ui.theme.FalconerColors
import dev.alifhasnain.falconer.ui.theme.MonoStyle
import dev.alifhasnain.falconer.ui.theme.methodColor
import dev.alifhasnain.falconer.ui.theme.statusColor

/** A small, quiet section label. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = FalconerColors.InkSecondary,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

/**
 * A tappable [SectionHeader] that toggles its content. Shows a chevron and an
 * optional [count] so the section's size stays visible even when collapsed
 * (PRODUCT principle 3 — faithful, never silently hidden).
 */
@Composable
fun CollapsibleSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
            contentDescription = if (expanded) "Collapse $title" else "Expand $title",
            tint = FalconerColors.InkSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = FalconerColors.InkSecondary,
        )
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = FalconerColors.Neutral,
            )
        }
    }
}

/** Colored, low-key HTTP method tag. */
@Composable
fun MethodChip(method: String, modifier: Modifier = Modifier) {
    val color = methodColor(method)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(method.uppercase(), color = color, style = MaterialTheme.typography.labelSmall)
    }
}

/** Status code (colored), or `ERR` for a transport error, or `···` in-flight. */
@Composable
fun StatusText(statusCode: Int?, error: String?, modifier: Modifier = Modifier) {
    val label: String
    val color = statusColor(statusCode)
    when {
        statusCode != null -> label = statusCode.toString()
        error != null -> return Text("ERR", color = FalconerColors.Red, style = MaterialTheme.typography.labelLarge, modifier = modifier)
        else -> return Text("···", color = FalconerColors.Neutral, style = MaterialTheme.typography.labelLarge, modifier = modifier)
    }
    Text(label, color = color, style = MaterialTheme.typography.labelLarge, modifier = modifier)
}

/** A label/value row. Redacted values are marked (PRODUCT principle 3). */
@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
) {
    val redacted = value == FalconerNativeConfig.REDACTED
    Row(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = FalconerColors.InkSecondary,
            modifier = Modifier.width(116.dp),
        )
        SelectionContainer {
            Text(
                value,
                style = if (mono) MonoStyle else MaterialTheme.typography.bodyMedium,
                color = if (redacted) FalconerColors.Amber else FalconerColors.Ink,
                fontStyle = if (redacted) FontStyle.Italic else FontStyle.Normal,
            )
        }
    }
}

/** Muted note for empty/marker states — never silently blank. */
@Composable
fun MutedNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MonoStyle,
        color = FalconerColors.InkSecondary,
        modifier = modifier.padding(16.dp),
    )
}

/** Renders a stored headers-JSON string as label/value rows. */
@Composable
fun HeadersBlock(headersJson: String?, modifier: Modifier = Modifier) {
    val headers = remember(headersJson) { Converters.jsonToHeaders(headersJson) }
    if (headers.isEmpty()) {
        MutedNote("(no headers)", modifier)
        return
    }
    Column(modifier) {
        headers.forEach { (key, value) -> KeyValueRow(key, value, mono = true) }
    }
}
