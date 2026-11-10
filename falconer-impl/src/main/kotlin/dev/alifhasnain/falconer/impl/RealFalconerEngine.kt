package dev.alifhasnain.falconer.impl

import android.content.Context
import dev.alifhasnain.falconer.channel.PayloadMapper
import dev.alifhasnain.falconer.config.FalconerNativeConfig
import dev.alifhasnain.falconer.data.FalconerDatabase
import dev.alifhasnain.falconer.data.PrefsLastCleanupStore
import dev.alifhasnain.falconer.data.RetentionManager
import dev.alifhasnain.falconer.data.TransactionRepository
import dev.alifhasnain.falconer.engine.FalconerEngine
import dev.alifhasnain.falconer.notification.NotificationHelper
import dev.alifhasnain.falconer.ui.FalconerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * The real, full-fat [FalconerEngine]: exactly the logic that used to live
 * inside `FalconerPlugin.onAttachedToEngine` / `onMethodCall` (DB open, retention
 * pass, notification collector, repository log/clear calls, [FalconerActivity] launch,
 * live count). Discovered by `ServiceLoader` when `:falconer-impl` is on the
 * classpath (the debug variant).
 *
 * The thin plugin keeps only the channel wiring and the notification-permission
 * flow; everything below is what it now delegates through the seam.
 */
class RealFalconerEngine : FalconerEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var repository: TransactionRepository? = null
    private var retention: RetentionManager? = null
    private var notifJob: Job? = null
    private var notificationHelper: NotificationHelper? = null

    @Volatile
    private var nativeConfig: FalconerNativeConfig = FalconerNativeConfig.DEFAULT

    override fun attach(context: Context) {
        val dao = FalconerDatabase.get(context).transactionDao()
        val retentionManager = RetentionManager(
            lastCleanup = PrefsLastCleanupStore(context),
            deleteBefore = dao::deleteBefore,
        )
        retention = retentionManager
        repository = TransactionRepository(dao, retentionManager) { nativeConfig }

        // Startup retention pass.
        scope.launch { repository?.cleanupOnStart() }

        // Throttled ongoing notification reflecting live capture.
        notificationHelper = NotificationHelper(context)
        // Newest few rows + COUNT(*), not the whole list: this re-queries on every
        // capture write, so its cost must not grow with the number of rows kept.
        val repo = repository ?: return
        notifJob = scope.launch {
            combine(
                repo.observeRecent(NotificationHelper.MAX_LINES.toLong()),
                repo.observeCount(),
            ) { recent, total -> recent to total }.conflate().collect { (recent, total) ->
                if (nativeConfig.showNotification && total > 0) {
                    notificationHelper?.show(recent, total)
                } else {
                    notificationHelper?.dismiss()
                }
                delay(NOTIFICATION_THROTTLE_MS)
            }
        }
    }

    override fun detach() {
        notifJob?.cancel()
        notificationHelper?.dismiss()
        scope.cancel()
    }

    override fun configure(args: Map<*, *>) {
        val config = FalconerNativeConfig.fromMap(args)
        nativeConfig = config
        retention?.period = config.retention
        scope.launch { repository?.cleanupOnStart() }
    }

    override fun logRequest(args: Map<*, *>) {
        if (!nativeConfig.enabled) return
        scope.launch { repository?.logRequest(PayloadMapper.parseRequest(args)) }
    }

    override fun logResponse(args: Map<*, *>) {
        if (!nativeConfig.enabled) return
        scope.launch { repository?.logResponse(PayloadMapper.parseResponse(args)) }
    }

    override fun logError(args: Map<*, *>) {
        if (!nativeConfig.enabled) return
        scope.launch { repository?.logError(PayloadMapper.parseError(args)) }
    }

    override fun clear() {
        scope.launch { repository?.clear() }
    }

    override fun launchUi(context: Context) {
        FalconerActivity.start(context)
    }

    override fun observeCount(): Flow<Int> = repository?.observeCount() ?: flowOf(0)

    private companion object {
        const val NOTIFICATION_THROTTLE_MS = 700L
    }
}
