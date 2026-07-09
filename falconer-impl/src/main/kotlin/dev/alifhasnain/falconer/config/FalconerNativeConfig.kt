package dev.alifhasnain.falconer.config

import dev.alifhasnain.falconer.data.RetentionPeriod

/**
 * Native-side configuration parsed from the `configure` channel map.
 *
 * Redaction and truncation already happen in Dart before the payload crosses
 * the channel (D2); this enforces them again as a defense-in-depth backstop so
 * a misbehaving caller cannot persist secrets or oversized bodies.
 */
data class FalconerNativeConfig(
    val enabled: Boolean,
    val redactHeaders: Set<String>, // lowercased
    val maxContentLength: Int,
    val retention: RetentionPeriod,
    val showNotification: Boolean,
) {
    /** Masks header values whose names match [redactHeaders] (case-insensitive). */
    fun redact(headers: Map<String, String>): Map<String, String> {
        if (redactHeaders.isEmpty()) return headers
        return headers.mapValues { (key, value) ->
            if (key.lowercase() in redactHeaders) REDACTED else value
        }
    }

    /** Truncates an over-cap body with a visible marker; [originalSize] is reported elsewhere. */
    fun truncate(body: String?, originalSize: Long?): String? {
        if (body == null || originalSize == null || originalSize <= maxContentLength) return body
        val head = if (body.length > maxContentLength) body.substring(0, maxContentLength) else body
        return "$head\n\n[Falconer: truncated — original $originalSize bytes, showing first $maxContentLength]"
    }

    companion object {
        const val REDACTED = "**redacted**"

        val DEFAULT_REDACT = setOf(
            "authorization",
            "cookie",
            "set-cookie",
            "proxy-authorization",
            "x-api-key",
            "x-auth-token",
        )

        val DEFAULT = FalconerNativeConfig(
            enabled = false,
            redactHeaders = DEFAULT_REDACT,
            maxContentLength = 250_000,
            retention = RetentionPeriod.ONE_DAY,
            showNotification = true,
        )

        // Keys mirror `ConfigKeys` in lib/src/platform/contract.dart.
        fun fromMap(map: Map<*, *>): FalconerNativeConfig {
            val redact = (map["redactHeaders"] as? List<*>)
                ?.mapNotNull { it as? String }
                ?.map { it.lowercase() }
                ?.toSet()
                ?: DEFAULT_REDACT
            return FalconerNativeConfig(
                enabled = map["enabled"] as? Boolean ?: false,
                redactHeaders = redact,
                maxContentLength = (map["maxContentLength"] as? Number)?.toInt() ?: 250_000,
                retention = RetentionPeriod.fromKey(map["retention"] as? String),
                showNotification = map["showNotification"] as? Boolean ?: true,
            )
        }
    }
}
