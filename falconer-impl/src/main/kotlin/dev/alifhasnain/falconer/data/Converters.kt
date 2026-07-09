package dev.alifhasnain.falconer.data

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Converts the headers `Map<String, String>` to/from the JSON string stored in
 * the entity. Used by the repository on write and the UI on read.
 *
 * Uses an explicit serializer so no kotlinx-serialization compiler plugin is
 * required, and runs in plain-JVM unit tests.
 */
object Converters {
    private val json = Json { ignoreUnknownKeys = true }
    private val headersSerializer = MapSerializer(String.serializer(), String.serializer())

    fun headersToJson(headers: Map<String, String>): String =
        json.encodeToString(headersSerializer, headers)

    fun jsonToHeaders(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) return emptyMap()
        return runCatching { json.decodeFromString(headersSerializer, value) }
            .getOrDefault(emptyMap())
    }
}
