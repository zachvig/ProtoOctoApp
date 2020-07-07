package de.crysxd.octoapp.base.ui.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

object NavigationResultMediator {

    private var resultCounter = AtomicInteger()
    private val liveDataIndex = mutableMapOf<Int, WeakReference<MutableLiveData<in Any?>>>()

    fun <T : Any?> registerResultCallback(): Pair<Int, LiveData<T?>> {
        val resultId = resultCounter.incrementAndGet()
        val liveData = MutableLiveData<T?>()
        liveDataIndex[resultId] = WeakReference(liveData as MutableLiveData<in Any?>)
        val wrappedLiveData: LiveData<T?> = Transformations.map(liveData) { it as T? }
        return Pair(resultId, wrappedLiveData)
    }

    fun <T : Any> postResult(resultId: Int, result: T?): Boolean {
        if (resultId >= 0) {
            val liveData = liveDataIndex[resultId]?.get()

            return when {
                liveData != null -> {
                    liveData.postValue(result)
                    true
                }
                else -> {
                    false
                }
            }
        } else {
            return false
        }
    }
}