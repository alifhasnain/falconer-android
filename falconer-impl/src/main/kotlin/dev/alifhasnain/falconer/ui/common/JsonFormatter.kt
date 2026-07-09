package dev.alifhasnain.falconer.ui.common

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Pretty-prints JSON bodies. Malformed JSON falls back to the raw text
 * unchanged — never throws, never hides what was captured (PRODUCT principle 3).
 */
object JsonFormatter {
    @OptIn(ExperimentalSerializationApi::class)
    private val pretty = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun format(body: String?): String {
        if (body.isNullOrBlank()) return ""
        return runCatching {
            val element = pretty.parseToJsonElement(body)
            pretty.encodeToString(JsonElement.serializer(), element)
        }.getOrDefault(body)
    }
}
