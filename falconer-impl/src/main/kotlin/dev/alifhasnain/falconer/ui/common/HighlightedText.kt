package dev.alifhasnain.falconer.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

/**
 * Case-insensitive in-body match finding and highlight building. Pure logic,
 * unit-tested headless; the composable that scrolls between matches lives in
 * the body viewer.
 */
object BodySearch {

    /** All non-overlapping match ranges of [query] in [text] (case-insensitive). */
    fun findMatches(text: String, query: String): List<IntRange> {
        if (query.isEmpty() || text.isEmpty()) return emptyList()
        val haystack = text.lowercase()
        val needle = query.lowercase()
        val out = mutableListOf<IntRange>()
        var index = haystack.indexOf(needle)
        while (index >= 0) {
            out.add(index until index + needle.length)
            index = haystack.indexOf(needle, index + needle.length)
        }
        return out
    }

    /**
     * Builds an [AnnotatedString] over [text] applying [matchStyle] to every
     * match and [currentStyle] to the match at [current].
     */
    fun highlight(
        text: String,
        matches: List<IntRange>,
        current: Int,
        matchStyle: SpanStyle,
        currentStyle: SpanStyle,
    ): AnnotatedString = buildAnnotatedString {
        append(text)
        matches.forEachIndexed { i, range ->
            addStyle(
                if (i == current) currentStyle else matchStyle,
                range.first,
                range.last + 1,
            )
        }
    }
}
