package dev.alifhasnain.falconer.noop

import android.content.Context
import dev.alifhasnain.falconer.engine.FalconerEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The inert mirror of [FalconerEngine]. Every method is a stub: no DB, no UI, no
 * notifications, no coroutines. Discovered by `ServiceLoader` when only
 * `:falconer-noop` is on the classpath (the release variant), so a release build
 * that merely depends on Falconer does zero inspector work and stores nothing —
 * the data-at-rest / data-exposure posture required under PCI-DSS
 * (RELEASE_STRIPPING.md §7).
 */
class NoOpFalconerEngine : FalconerEngine {
    override fun attach(context: Context) = Unit
    override fun detach() = Unit
    override fun configure(args: Map<*, *>) = Unit
    override fun logRequest(args: Map<*, *>) = Unit
    override fun logResponse(args: Map<*, *>) = Unit
    override fun logError(args: Map<*, *>) = Unit
    override fun clear() = Unit
    override fun launchUi(context: Context) = Unit          // no UI in release
    override fun observeCount(): Flow<Int> = flowOf(0)
}
