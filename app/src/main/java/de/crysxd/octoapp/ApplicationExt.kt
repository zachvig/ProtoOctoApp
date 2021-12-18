package de.crysxd.octoapp

import android.app.Application
import de.crysxd.octoapp.base.di.BaseInjector
import timber.log.Timber
import java.io.IOException

fun Application.setupFirebaseCrashFix() {
    val handler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        var isFirebaseCrash = false
        var exception: Throwable? = e
        while (exception != null) {
            isFirebaseCrash = isFirebaseCrash || exception.isFirebaseCrash()
            exception = exception.cause
        }

        if (isFirebaseCrash) {
            Timber.e("Firebase caused app to crash, disabling push notifications")
            BaseInjector.get().octoPreferences().suppressRemoteMessageInitialization = true
        }

        handler?.uncaughtException(t, e)
    }
}

private fun Throwable.isFirebaseCrash() = this is IOException && listOf(
    "SERVICE_NOT_AVAILABLE",
    "FIS_AUTH_ERROR",
    "AUTHENTICATION_FAILED",
    "MISSING_INSTANCEID_SERVICE"
).contains(message)