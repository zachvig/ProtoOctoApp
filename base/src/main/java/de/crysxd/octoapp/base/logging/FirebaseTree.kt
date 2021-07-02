package de.crysxd.octoapp.base.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfigClientException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.IOException
import java.net.URISyntaxException

class FirebaseTree(
    private val mask: SensitiveDataMask
) : Timber.DebugTree() {

    private val lock = Mutex()
    private var lastException: Throwable? = null

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.INFO) {
            GlobalScope.launch(Dispatchers.IO) {
                lock.withLock {

                    FirebaseCrashlytics.getInstance().log("$tag | ${mask.mask(message)}")

                    if (shouldLog(t) && t != null) {
                        lastException = t
                        if (priority >= Log.ERROR) {
                            FirebaseCrashlytics.getInstance().recordException(t)
                        } else {
                            FirebaseCrashlytics.getInstance().log("ERROR: ${t::class.java.name}: ${mask.mask(t.message ?: "")}")
                        }
                    }
                }
            }
        }
    }

    private fun shouldLog(t: Throwable?) = t != null &&
            t != lastException &&
            t !is IOException &&
            t !is CancellationException &&
            t !is FirebaseRemoteConfigClientException &&
            t !is URISyntaxException
}