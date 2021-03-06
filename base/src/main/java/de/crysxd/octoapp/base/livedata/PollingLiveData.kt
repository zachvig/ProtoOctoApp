package de.crysxd.octoapp.base.livedata

import android.os.Handler
import androidx.lifecycle.LiveData
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Runnable

class PollingLiveData<T>(
    private val interval: Long = 1000,
    private val action: suspend () -> T
) : LiveData<PollingLiveData.Result<T>>() {

    private var job = Job()
    private val handler = Handler()
    private val runnable = Runnable { poll() }

    override fun onActive() {
        super.onActive()
        job = Job()
        poll()
    }

    override fun onInactive() {
        super.onInactive()
        job.cancel()
        handler.removeCallbacks(runnable)
    }

    private fun poll(): Job = GlobalScope.launch(job + Dispatchers.IO) {
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