package dev.alifhasnain.falconer.engine

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * The only surface the thin `falconer` plugin knows about.
 *
 * Both `:falconer-impl` and `:falconer-noop` provide a concrete implementation;
 * `ServiceLoader` picks the one linked for the current build variant
 * (`debugImplementation falconer-impl` / `releaseImplementation falconer-noop`).
 * Because the plugin only ever references this interface, a release build that
 * links the no-op has no live edge to Compose, SQLDelight, [FalconerActivity],
 * or the notification stack — R8 strips them and the no-op AAR contributes no
 * manifest components (RELEASE_STRIPPING.md §3).
 *
 * Every method mirrors one branch of the plugin's former `onMethodCall` /
 * `onAttachedToEngine`; the no-op implements them all as inert stubs.
 */
interface FalconerEngine {
    /**
     * Replaces the eager init that used to live in `onAttachedToEngine`
     * (DB open, startup retention pass, notification collector). The no-op does
     * nothing here — so a release build performs zero DB I/O and starts no
     * coroutine (RELEASE_STRIPPING.md §6).
     */
    fun attach(context: Context)

    /** Tears down anything [attach] started. Inert in the no-op. */
    fun detach()

    fun configure(args: Map<*, *>)
    fun logRequest(args: Map<*, *>)
    fun logResponse(args: Map<*, *>)
    fun logError(args: Map<*, *>)
    fun clear()

    /** Launches the inspection UI. The no-op has no UI, so this is a stub. */
    fun launchUi(context: Context)

    /** Backs the `falconer/transactionCount` EventChannel. No-op emits `0`. */
    fun observeCount(): Flow<Int>
}
