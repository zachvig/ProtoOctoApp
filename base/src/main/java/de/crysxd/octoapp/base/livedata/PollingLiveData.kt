package de.crysxd.octoapp.base.livedata

import android.os.Handler
import androidx.lifecycle.LiveData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class PollingLiveData<T>(
    private val interval: Long = 1000,
    private val action: suspend () -> T
) : LiveData<PollingLiveData.Result<T>>() {

    private val handler = Handler()
    private val runnable = Runnable { poll() }

    override fun onActive() {
        super.onActive()
        poll()
    }

    override fun onInactive() {
        super.onInactive()
        handler.removeCallbacks(runnable)
    }

    private fun poll(): Job = GlobalScope.launch {
        try {
            postValue(Result.Success(action()))
        } catch (e: Exception) {
            Timber.e(e)
            postValue(Result.Failure(e))
        }

        if (this@PollingLiveData.hasActiveObservers()) {
            handler.postDelayed(runnable, interval)
        }
    }

    sealed class Result<T> {
        data class Success<T>(val result: T) : Result<T>()
        data class Failure<T>(val exception: Exception): Result<T>()
    }
}