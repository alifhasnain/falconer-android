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
    fun forever_neverDeletes() = runTest {
        val cuts = mutableListOf<Long>()
        val rm = RetentionManager(MemStore(), { cuts.add(it) }, now = { 1_000_000L })
            .apply { period = RetentionPeriod.FOREVER }

        rm.cleanupNow()

        assertTrue(cuts.isEmpty())
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
        assertEquals(RetentionPeriod.FOREVER, RetentionPeriod.fromKey("forever"))
        assertEquals(RetentionPeriod.ONE_DAY, RetentionPeriod.fromKey(null))
        assertEquals(RetentionPeriod.ONE_DAY, RetentionPeriod.fromKey("bogus"))
    }
}
