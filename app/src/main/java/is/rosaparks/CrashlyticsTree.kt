package `is`.rosaparks

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class CrashlyticsTree : Timber.Tree() {
    // Lazy + null-safe — Símaver beta35 shipped with eager
    // `FirebaseCrashlytics.getInstance()` in init, which crashed every launch
    // with "FirebaseCrashlytics component is not present" when R8 minification
    // stripped the Crashlytics component registrar in release builds. Resolve
    // lazily and tolerate the SDK being unavailable so a Crashlytics
    // misconfiguration can never break the whole app.
    private val crashlytics: FirebaseCrashlytics? by lazy {
        runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()
    }

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        if (priority < Log.INFO) return
        val sink = crashlytics ?: return

        val prefix =
            when (priority) {
                Log.WARN -> "W"
                Log.ERROR -> "E"
                Log.ASSERT -> "A"
                else -> "I"
            }
        runCatching { sink.log("$prefix/${tag ?: "App"}: $message") }

        if (t != null && priority >= Log.WARN) {
            runCatching { sink.recordException(t) }
        } else if (priority >= Log.ERROR) {
            runCatching { sink.recordException(TimberLogException(message)) }
        }
    }

    private class TimberLogException(
        message: String,
    ) : Exception(message)
}
