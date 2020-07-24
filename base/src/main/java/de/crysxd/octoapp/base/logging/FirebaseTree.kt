package de.crysxd.octoapp.base.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class FirebaseTree : Timber.DebugTree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        FirebaseCrashlytics.getInstance().log("$tag | $message")
        t?.let { FirebaseCrashlytics.getInstance().recordException(it) }
    }
}