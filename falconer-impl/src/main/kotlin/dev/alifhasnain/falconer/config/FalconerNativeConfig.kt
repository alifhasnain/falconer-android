package dev.alifhasnain.falconer.config

import dev.alifhasnain.falconer.data.RetentionPeriod

/**
 * Native-side configuration parsed from the `configure` channel map.
 *
 * Redaction and truncation already happen in Dart before the payload crosses
 * the channel; this enforces them again as a defense-in-depth backstop so a
 * misbehaving caller cannot persist secrets or oversized bodies.
 *
 * [enabled] is the resolved `effectiveEnabled` from Dart. `RealFalconerEngine`
 * drops every ingest while it is `false`, so capture can be switched off from
 * Dart without relying on the interceptor's own short-circuit.
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

        /**
         * The pre-`configure` fallback, used only until the first `configure`
         * map arrives. Mirrors iOS `FalconerNativeConfig.defaults`.
         *
         * `enabled = true` so capture works for a consumer who adds the
         * interceptor but never calls `Falconer.configure()`. That is safe
         * because this artifact (`falconer-impl`) is linked in DEBUG builds only
         * — a release build links `falconer-noop` and contains none of this.
         *
         * Note this differs from [fromMap]'s own `enabled` fallback, which is
         * `false`: an explicit but malformed `configure` map should fail closed,
         * whereas "no configure yet" in a debug-only artifact should capture.
         *
         * Redaction and retention stay conservative here so anything captured
         * before `configure` lands is masked with the strong default set and
         * swept on the short window.
         */
        val DEFAULT = FalconerNativeConfig(
            enabled = true,
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
