package dev.alifhasnain.falconer.sample

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import dev.alifhasnain.falconer.channel.PayloadKeys
import dev.alifhasnain.falconer.engine.FalconerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.ServiceLoader
import java.util.UUID

/**
 * Sample host that exercises the native library WITHOUT Flutter — through the
 * same seam the plugin uses: it only ever touches [FalconerEngine] +
 * `ServiceLoader`, never a concrete impl. So this screen IS the Method A demo:
 *
 *   debug build   -> :falconer-impl on the classpath -> RealFalconerEngine
 *   release build -> :falconer-noop on the classpath -> NoOpFalconerEngine
 *
 * The status line prints which engine loaded, so the variant swap is visible on
 * screen. In a release build "Open inspector" does nothing and the count stays
 * 0 — because the inspector is physically absent, not merely disabled.
 */
class MainActivity : Activity() {

    private val engine: FalconerEngine =
        ServiceLoader.load(FalconerEngine::class.java, FalconerEngine::class.java.classLoader)
            .firstOrNull() ?: error("No FalconerEngine on the classpath")

    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine.attach(applicationContext)

        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, 0, 0, 48)
        }
        render(0)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 96, 48, 48)
            addView(status)
            addView(button("Inject sample traffic") { injectSample() })
            addView(button("Open inspector") { engine.launchUi(this@MainActivity) })
            addView(button("Clear") { engine.clear() })
        }
        setContentView(root)

        // In release this Flow is flowOf(0) (the no-op), so the count never moves.
        scope.launch {
            engine.observeCount().collectLatest { count -> render(count) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        engine.detach()
        super.onDestroy()
    }

    private fun render(count: Int) {
        status.text = "Engine: ${engine.javaClass.simpleName}\nCaptured: $count"
    }

    /** Feeds one fake request/response pair through the engine (keys mirror the
     *  channel contract in :falconer-core). Real capture crosses the same maps. */
    private fun injectSample() {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        engine.logRequest(
            mapOf(
                PayloadKeys.ID to id,
                PayloadKeys.STARTED_AT to now,
                PayloadKeys.METHOD to "GET",
                PayloadKeys.URL to "https://api.example.com/v1/ping",
                PayloadKeys.HOST to "api.example.com",
                PayloadKeys.PATH to "/v1/ping",
                PayloadKeys.SCHEME to "https",
                PayloadKeys.REQUEST_HEADERS to mapOf("accept" to "application/json"),
                PayloadKeys.REQUEST_BODY_KIND to "none",
            ),
        )
        engine.logResponse(
            mapOf(
                PayloadKeys.ID to id,
                PayloadKeys.COMPLETED_AT to now + 123L,
                PayloadKeys.TOOK_MS to 123L,
                PayloadKeys.STATUS_CODE to 200,
                PayloadKeys.STATUS_MESSAGE to "OK",
                PayloadKeys.PROTOCOL to "http/1.1",
                PayloadKeys.RESPONSE_HEADERS to mapOf("content-type" to "application/json"),
                PayloadKeys.RESPONSE_CONTENT_TYPE to "application/json",
                PayloadKeys.RESPONSE_BODY to "{\"ok\":true}",
                PayloadKeys.RESPONSE_BODY_KIND to "text",
            ),
        )
    }

    private fun button(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setOnClickListener { onClick() }
        }
}
