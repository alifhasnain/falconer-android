package dev.alifhasnain.falconer.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class RetentionManagerTest {

    private class MemStore(var value: Long = 0L) : LastCleanupStore {
        override fun get(): Long = value
        override fun set(value: Long) {
            this.value = value
        }
    }

    @Test
    fun everyWindowIsBounded_soCleanupAlwaysSweeps() = runTest {
        // There is no unbounded window: whatever the host app configures, a sweep
        // runs and a cutoff is applied.
        for (period in RetentionPeriod.entries) {
            val cuts = mutableListOf<Long>()
            val rm = RetentionManager(MemStore(), { cuts.add(it) }, now = { 10_000_000_000L })
                .apply { this.period = period }

            rm.cleanupNow()

            assertEquals(listOf(10_000_000_000L - period.millis), cuts)
            assertTrue(period.millis <= RetentionPeriod.ONE_MONTH.millis)
        }
    }

    @Test
    fun oneHour_deletesBeforeCutoff_andStampsLastCleanup() = runTest {
        val cuts = mutableListOf<Long>()
        val store = MemStore()
        val rm = RetentionManager(store, { cuts.add(it) }, now = { 10_000_000L })
            .apply { period = RetentionPeriod.ONE_HOUR }

        rm.cleanupNow()

        assertEquals(listOf(10_000_000L - 3_600_000L), cuts)
        assertEquals(10_000_000L, store.value)
    }

    @Test
    fun onWrite_throttlesToMinInterval() = runTest {
        val cuts = mutableListOf<Long>()
        val store = MemStore(0L)
        var clock = 100_000L
        val rm = RetentionManager(
            lastCleanup = store,
            deleteBefore = { cuts.add(it) },
            now = { clock },
            minIntervalMs = 60_000L,
        ).apply { period = RetentionPeriod.ONE_DAY }

        rm.onWrite() // 100000 - 0 >= 60000 -> runs
        assertEquals(1, cuts.size)

        rm.onWrite() // 100000 - 100000 = 0 < 60000 -> skipped
        assertEquals(1, cuts.size)

        clock += 60_000L
        rm.onWrite() // 160000 - 100000 = 60000 >= 60000 -> runs
        assertEquals(2, cuts.size)
    }

    @Test
    fun retentionPeriod_fromKey_mapsKnownKeysAndDefaults() {
        assertEquals(RetentionPeriod.ONE_HOUR, RetentionPeriod.fromKey("oneHour"))
        assertEquals(RetentionPeriod.ONE_DAY, RetentionPeriod.fromKey("oneDay"))
        assertEquals(RetentionPeriod.ONE_WEEK, RetentionPeriod.fromKey("oneWeek"))
        assertEquals(RetentionPeriod.ONE_MONTH, RetentionPeriod.fromKey("oneMonth"))
        // Every Dart key must map to a distinct window — a missing branch would
        // silently fall through to ONE_DAY.
        assertEquals(
            RetentionPeriod.entries.size,
            listOf("oneHour", "oneDay", "oneWeek", "oneMonth")
                .map { RetentionPeriod.fromKey(it) }.toSet().size,
        )
        assertEquals(RetentionPeriod.ONE_DAY, RetentionPeriod.fromKey(null))
        assertEquals(RetentionPeriod.ONE_DAY, RetentionPeriod.fromKey("bogus"))
        // The retired key degrades to one day (deletes more), never to "keep all".
        assertEquals(RetentionPeriod.ONE_DAY, RetentionPeriod.fromKey("forever"))
    }
}
