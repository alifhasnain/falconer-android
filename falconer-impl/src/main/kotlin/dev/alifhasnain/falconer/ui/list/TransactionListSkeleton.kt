package dev.alifhasnain.falconer.ui.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.alifhasnain.falconer.ui.common.ShimmerBlock
import dev.alifhasnain.falconer.ui.common.ShimmerContainer
import dev.alifhasnain.falconer.ui.theme.FalconerColors

/**
 * Uneven path widths keep the placeholder reading as a list of *different*
 * requests rather than a repeating pattern. Fixed (not random) so the skeleton
 * is stable across recompositions.
 */
private val PathFractions = listOf(0.62f, 0.44f, 0.78f, 0.53f, 0.70f, 0.38f, 0.66f, 0.50f)

/**
 * Loading placeholder for the transaction list.
 *
 * Geometry mirrors `TransactionRow` exactly — same paddings, same block heights,
 * same divider — so the real rows land where the placeholders were with no
 * visible reflow (PRODUCT principle 2: the eye is already parked on the hot path).
 */
@Composable
fun TransactionListSkeleton(modifier: Modifier = Modifier, rows: Int = 7) {
    ShimmerContainer(
        modifier = modifier.fillMaxSize(),
        contentDescription = "Loading transactions",
    ) {
        Column(Modifier.fillMaxSize()) {
            repeat(rows) { index ->
                SkeletonRow(PathFractions[index % PathFractions.size])
                HorizontalDivider(color = FalconerColors.Outline)
            }
        }
    }
}

@Composable
private fun SkeletonRow(pathFraction: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Method chip.
                ShimmerBlock(Modifier.width(38.dp), height = 16.dp)
                Spacer(Modifier.width(8.dp))
                // Path.
                ShimmerBlock(Modifier.fillMaxWidth(pathFraction), height = 13.dp)
            }
            Spacer(Modifier.height(6.dp))
            // Host.
            ShimmerBlock(Modifier.fillMaxWidth(pathFraction * 0.6f), height = 10.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            // Status code.
            ShimmerBlock(Modifier.width(26.dp), height = 14.dp)
            Spacer(Modifier.height(6.dp))
            // Duration · size.
            ShimmerBlock(Modifier.width(76.dp), height = 10.dp)
        }
    }
}
