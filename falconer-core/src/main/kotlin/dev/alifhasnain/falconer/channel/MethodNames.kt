package dev.alifhasnain.falconer.channel

/**
 * Method-channel method names — the shared Dart <-> Kotlin contract.
 *
 * Mirrors the string keys used in `lib/src/platform/method_channel_falconer.dart`.
 * Renaming a key here must be matched on the Dart side (Phase 3 adds a
 * contract test that fails on drift).
 */
object MethodNames {
    const val PING = "ping"
    const val CONFIGURE = "configure"
    const val LOG_REQUEST = "logRequest"
    const val LOG_RESPONSE = "logResponse"
    const val LOG_ERROR = "logError"
    const val CLEAR_TRANSACTIONS = "clearTransactions"
    const val LAUNCH_UI = "launchUi"
    const val REQUEST_NOTIFICATION_PERMISSION = "requestNotificationPermission"

    /** Method channel name. */
    const val CHANNEL = "falconer"

    /** Event channel carrying the live transaction count. */
    const val TRANSACTION_COUNT_CHANNEL = "falconer/transactionCount"
}
