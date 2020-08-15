package de.crysxd.octoapp.base.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

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

                    if (t != null && t != lastException) {
                        lastException = t
                        if (priority >= Log.ERROR) {
                            FirebaseCrashlytics.getInstance().recordException(t)
                        } else {
                            FirebaseCrashlytics.getInstance().log("ERROR: ${t::class.java.name}: ${t.message}")
                        }
                    }
                }
            }
        }
    }
}