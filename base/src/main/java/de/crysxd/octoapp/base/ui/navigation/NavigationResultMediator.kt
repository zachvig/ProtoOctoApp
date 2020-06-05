package de.crysxd.octoapp.base.ui.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

object NavigationResultMediator {

    private var resultCounter = AtomicInteger()
    private val liveDataIndex = mutableMapOf<Int, WeakReference<MutableLiveData<in Any>>>()
    private val callbackIndex = mutableMapOf<Int, WeakReference<(Any) -> Unit>>()

    fun <T : Any> registerResultCallback(): Pair<Int, LiveData<T>> {
        val resultId = resultCounter.incrementAndGet()
        val liveData = MutableLiveData<T>()
        liveDataIndex[resultId] = WeakReference(liveData as MutableLiveData<in Any>)
        val wrappedLiveData: LiveData<T> = Transformations.map(liveData) { it as T }
        return Pair(resultId, wrappedLiveData)
    }

    fun <T: Any> registerResultCallback(callback: (T) -> Any): Int {
        val resultId = resultCounter.incrementAndGet()
        callbackIndex[resultId] = WeakReference(callback as (Any) -> Unit)
        return resultId
    }

    fun <T: Any> postResult(resultId: Int, result: T): Boolean {
        val liveData = liveDataIndex[resultId]?.get()
        val callback = callbackIndex[resultId]?.get()

        return when {
            liveData != null -> {
                liveData.postValue(result)
                true
            }
            callback != null -> {
                callback(result)
                true
            }
            else -> {
                false
            }
         }
    }
}