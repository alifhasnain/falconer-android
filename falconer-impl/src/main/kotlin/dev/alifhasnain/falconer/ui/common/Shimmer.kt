package dev.alifhasnain.falconer.ui.common

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.alifhasnain.falconer.ui.theme.FalconerColors

/**
 * One sweep across the whole placeholder group, not a per-block pulse.
 *
 * PRODUCT principle 4 (restrained polish): the motion exists to say "the shape
 * of the data is already known, the data itself is a moment away" — a single
 * unhurried light pass, no bounce, no color.
 */
private const val SWEEP_DURATION_MS = 1500

/** Width of the moving highlight band, as a fraction of the container width. */
private const val BAND_FRACTION = 0.55f

/** Placeholder fill — the theme outline reads as "structure without content". */
private val PlaceholderColor = FalconerColors.Outline

/** The light that passes over the placeholders. */
private val HighlightColor = Color.White.copy(alpha = 0.85f)

/**
 * Test seam — provide `false` to render placeholders static.
 *
 * An infinite animation keeps Compose's idling resource permanently busy, so
 * `waitForIdle()` never settles and the test hangs until CI cancels it (the same
 * trap as the blinking text-field cursor documented in `ComposeUiTest`).
 */
val LocalShimmerAnimationEnabled = staticCompositionLocalOf { true }

/**
 * Wraps a group of [ShimmerBlock]s and runs a single diagonal light sweep across
 * all of them at once, so the group reads as one surface rather than a grid of
 * independently blinking boxes.
 *
 * The sweep is drawn with [BlendMode.SrcAtop] inside an offscreen layer, so the
 * highlight lands only on pixels the placeholders actually painted — the
 * background stays untouched.
 *
 * Honours the system animator scale (Developer Options "Animator duration
 * scale: off" / accessibility motion reduction): with animations disabled the
 * placeholders render static, which also keeps Compose UI tests idle-able —
 * an infinite animation never lets `waitForIdle()` settle.
 */
@Composable
fun ShimmerContainer(
    modifier: Modifier = Modifier,
    contentDescription: String = "Loading",
    content: @Composable () -> Unit,
) {
    val label = contentDescription
    val base = modifier.semantics(mergeDescendants = true) { this.contentDescription = label }

    if (!animationsEnabled()) {
        Box(base) { content() }
        return
    }

    val transition = rememberInfiniteTransition(label = "falconerShimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SWEEP_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "falconerShimmerSweep",
    )

    Box(
        base
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val band = size.width * BAND_FRACTION
                // Travel from fully off the left edge to fully off the right.
                val x = -band + progress * (size.width + band)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, HighlightColor, Color.Transparent),
                        start = Offset(x, 0f),
                        end = Offset(x + band, size.height),
                    ),
                    blendMode = BlendMode.SrcAtop,
                )
            },
    ) { content() }
}

/**
 * A single placeholder bar. Width comes from [modifier] (`width(…)` or
 * `fillMaxWidth(fraction)`) so callers can mirror the real row's proportions.
 */
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
    corner: Dp = 4.dp,
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(corner))
            .background(PlaceholderColor),
    )
}

/**
 * False when a test opted out, when the platform has animations turned off
 * (accessibility / Developer Options), or when rendering a `@Preview`.
 */
@Composable
private fun animationsEnabled(): Boolean {
    if (!LocalShimmerAnimationEnabled.current) return false
    if (LocalInspectionMode.current) return false
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
}
