package de.crysxd.octoapp.base.ui.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import java.util.concurrent.atomic.AtomicInteger

object NavigationResultMediator {

    private var resultCounter = AtomicInteger()
    private val index = mutableMapOf<Int, MutableLiveData<in Any>>()

    fun <T : Any> registerResultCallback(): Pair<Int, LiveData<T>> {
        val resultId = resultCounter.incrementAndGet()
        val liveData = MutableLiveData<T>()
        index[resultId] = liveData as MutableLiveData<in Any>
        val wrappedLiveData: LiveData<T> = Transformations.map(liveData) { it as T }
        return Pair(resultId, wrappedLiveData)
    }

    fun <T: Any> postResult(resultId: Int, result: T): Boolean {
        index[resultId]?.postValue(result) ?: return false
        return true
    }
}