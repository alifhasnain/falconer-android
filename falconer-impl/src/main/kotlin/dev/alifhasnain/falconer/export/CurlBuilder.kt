package dev.alifhasnain.falconer.export

import dev.alifhasnain.falconer.data.Converters
import dev.alifhasnain.falconer.data.HttpTransactionEntity

/**
 * Reproduces a captured request as a runnable `curl` command. Headers are taken
 * as stored (already redacted), so the command never leaks secrets — it will
 * carry `**redacted**` where auth headers were masked. Pure string logic →
 * portable to iOS later.
 */
object CurlBuilder {

    fun build(tx: HttpTransactionEntity): String {
        val sb = StringBuilder()
        sb.append("curl -X ").append(tx.method).append(' ').append(quote(tx.url))

        for ((name, value) in Converters.jsonToHeaders(tx.requestHeaders)) {
            sb.append(" \\\n  -H ").append(quote("$name: $value"))
        }

        val body = tx.requestBody
        // Only text/json bodies reproduce faithfully as --data. Multipart/image/
        // unsupported are not reconstructable, so they are intentionally omitted.
        if (!body.isNullOrEmpty() && (tx.requestBodyKind == "text" || tx.requestBodyKind == "json")) {
            sb.append(" \\\n  --data ").append(quote(body))
        }

        return sb.toString()
    }

    /** Wraps [s] in a shell single-quoted string, escaping embedded quotes. */
    private fun quote(s: String): String = "'" + s.replace("'", "'\\''") + "'"
}
