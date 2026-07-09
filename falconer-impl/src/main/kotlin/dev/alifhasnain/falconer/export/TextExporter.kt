package dev.alifhasnain.falconer.export

import dev.alifhasnain.falconer.data.Converters
import dev.alifhasnain.falconer.data.HttpTransactionEntity
import dev.alifhasnain.falconer.ui.common.Format

/**
 * A full, human-readable request+response dump. Image bytes are excluded and
 * replaced with `[image omitted]`. Truncation/redaction markers stored in the
 * body/headers carry through verbatim. Pure string logic → portable.
 */
object TextExporter {

    private const val IMAGE_PLACEHOLDER = "[image omitted]"

    fun build(tx: HttpTransactionEntity): String = buildString {
        appendLine("=== Falconer transaction ===")
        appendLine("${tx.method} ${tx.url}")
        appendLine("Status: ${statusLine(tx)}   (${Format.duration(tx.tookMs)})")
        appendLine("Started: ${Format.time(tx.startedAt)}")
        appendLine()

        appendLine("--- Request headers ---")
        appendHeaders(tx.requestHeaders)
        appendLine()
        appendLine("--- Request body (${tx.requestBodyKind}) ---")
        appendLine(bodyText(tx.requestBodyKind, tx.requestBody, imageBytes = null))
        appendLine()

        appendLine("--- Response headers ---")
        appendHeaders(tx.responseHeaders)
        appendLine()
        appendLine("--- Response body (${tx.responseBodyKind ?: "—"}) ---")
        appendLine(bodyText(tx.responseBodyKind, tx.responseBody, tx.responseImageBytes))

        if (tx.error != null) {
            appendLine()
            appendLine("--- Error ---")
            appendLine(tx.error)
        }
    }

    private fun StringBuilder.appendHeaders(json: String?) {
        val headers = Converters.jsonToHeaders(json)
        if (headers.isEmpty()) {
            appendLine("(none)")
        } else {
            headers.forEach { (name, value) -> appendLine("$name: $value") }
        }
    }

    private fun statusLine(tx: HttpTransactionEntity): String = when {
        tx.statusCode != null -> "${tx.statusCode} ${tx.statusMessage ?: ""}".trim()
        tx.error != null -> "Failed"
        else -> "—"
    }

    private fun bodyText(kind: String?, body: String?, imageBytes: ByteArray?): String = when {
        imageBytes != null || kind == "image" -> IMAGE_PLACEHOLDER
        body.isNullOrEmpty() -> "(no body)"
        else -> body
    }
}
