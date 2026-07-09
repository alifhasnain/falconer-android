package dev.alifhasnain.falconer.config

import dev.alifhasnain.falconer.data.RetentionPeriod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class FalconerNativeConfigTest {

    @Test
    fun redact_masksMatchedHeadersCaseInsensitively() {
        val cfg = FalconerNativeConfig.DEFAULT
        val headers = mapOf(
            "Authorization" to "Bearer secret",
            "AUTHORIZATION" to "Bearer secret2",
            "Accept" to "application/json",
        )

        val redacted = cfg.redact(headers)

        assertEquals("**redacted**", redacted["Authorization"])
        assertEquals("**redacted**", redacted["AUTHORIZATION"])
        assertEquals("application/json", redacted["Accept"])
    }

    @Test
    fun truncate_underCap_returnsBodyUnchanged() {
        val cfg = FalconerNativeConfig.DEFAULT.copy(maxContentLength = 100)
        assertEquals("short", cfg.truncate("short", 5L))
    }

    @Test
    fun truncate_overCap_appendsMarkerAndKeepsHead() {
        val cfg = FalconerNativeConfig.DEFAULT.copy(maxContentLength = 4)
        val result = cfg.truncate("0123456789", 10L)!!
        assertTrue(result.startsWith("0123"))
        assertTrue(result.contains("truncated"))
        assertTrue(result.contains("original 10 bytes"))
    }

    @Test
    fun truncate_nullBody_isNull() {
        assertNull(FalconerNativeConfig.DEFAULT.truncate(null, 999_999L))
    }

    @Test
    fun fromMap_parsesEveryField() {
        val map = hashMapOf<String, Any?>(
            "enabled" to true,
            "maxContentLength" to 1234,
            "redactHeaders" to listOf("X-Custom", "Authorization"),
            "retention" to "oneWeek",
            "showNotification" to false,
        )

        val cfg = FalconerNativeConfig.fromMap(map)

        assertTrue(cfg.enabled)
        assertEquals(1234, cfg.maxContentLength)
        assertEquals(setOf("x-custom", "authorization"), cfg.redactHeaders)
        assertEquals(RetentionPeriod.ONE_WEEK, cfg.retention)
        assertEquals(false, cfg.showNotification)
    }

    @Test
    fun fromMap_missingFields_fallBackToDefaults() {
        val cfg = FalconerNativeConfig.fromMap(hashMapOf<String, Any?>())

        assertEquals(false, cfg.enabled)
        assertEquals(250_000, cfg.maxContentLength)
        assertEquals(FalconerNativeConfig.DEFAULT_REDACT, cfg.redactHeaders)
        assertEquals(RetentionPeriod.ONE_DAY, cfg.retention)
        assertTrue(cfg.showNotification)
    }
}
