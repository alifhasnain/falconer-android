package dev.alifhasnain.falconer.data

import android.content.Context

/**
 * Retention windows. `FOREVER` never deletes.
 *
 * Rolling durations measured back from now, not calendar units: `ONE_MONTH` is a
 * fixed 30 days. Mirrors Dart `RetentionPeriod` and Swift `RetentionWindow` —
 * all three must gain a value together (see [fromKey]).
 */
enum class RetentionPeriod(val millis: Long?) {
    ONE_HOUR(3_600_000L),
    ONE_DAY(86_400_000L),
    ONE_WEEK(604_800_000L),
    ONE_MONTH(2_592_000_000L), // 30 days
    FOREVER(null);

    /** Cutoff timestamp: rows older than this are stale. Null means keep all. */
    fun cutoffOrNull(now: Long): Long? = millis?.let { now - it }

    companion object {
        /**
         * Maps the Dart `RetentionPeriod` enum key; falls back to [ONE_DAY].
         *
         * The fallback is SILENT, so a key Dart sends but this map lacks would
         * sweep at one day with no error. Keep in step with Dart's enum.
         */
        fun fromKey(key: String?): RetentionPeriod = when (key) {
            "oneHour" -> ONE_HOUR
            "oneDay" -> ONE_DAY
            "oneWeek" -> ONE_WEEK
            "oneMonth" -> ONE_MONTH
            "forever" -> FOREVER
            else -> ONE_DAY
        }
    }
}

/** Persists the last-cleanup timestamp so on-write cleanup can be throttled. */
interface LastCleanupStore {
    fun get(): Long
    fun set(value: Long)
}

/** SharedPreferences-backed [LastCleanupStore]. */
class PrefsLastCleanupStore(context: Context) : LastCleanupStore {
    private val prefs =
        context.applicationContext.getSharedPreferences("falconer", Context.MODE_PRIVATE)

    override fun get(): Long = prefs.getLong(KEY, 0L)
    override fun set(value: Long) = prefs.edit().putLong(KEY, value).apply()

    private companion object {
        const val KEY = "lastCleanupAt"
    }
}

/**
 * Deletes stale transactions (D6): runs a full pass on startup/configure, and a
 * throttled pass after writes (no WorkManager dependency in v1).
 *
 * Dependencies are injected so the throttle/period logic is unit-testable
 * without Android.
 */
class RetentionManager(
    private val lastCleanup: LastCleanupStore,
    private val deleteBefore: suspend (Long) -> Unit,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val minIntervalMs: Long = 60_000L,
) {
    @Volatile
    var period: RetentionPeriod = RetentionPeriod.ONE_DAY

    /** Unconditional cleanup (startup / configure). No-op for [RetentionPeriod.FOREVER]. */
    suspend fun cleanupNow() {
        val cutoff = period.cutoffOrNull(now()) ?: return
        deleteBefore(cutoff)
        lastCleanup.set(now())
    }

    /** Throttled cleanup after a write — runs at most once per [minIntervalMs]. */
    suspend fun onWrite() {
        if (now() - lastCleanup.get() >= minIntervalMs) {
            cleanupNow()
        }
    }
}
