package de.crysxd.octoapp.base.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import de.crysxd.octoapp.base.models.Event
import kotlinx.coroutines.CoroutineExceptionHandler
import timber.log.Timber

abstract class BaseViewModel : ViewModel() {

    private val mutableErrorLiveData = MutableLiveData<Event<Throwable>>()
    val errorLiveData = Transformations.map(mutableErrorLiveData) { it }
    protected val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        postException(e)
        Timber.e(e)
    }

    private val mutableMessages = MutableLiveData<Event<OctoActivity.Message>>()
    val messages = Transformations.map(mutableMessages) { it }

    lateinit var navContoller: NavController

    protected fun postException(e: Throwable) {
        mutableErrorLiveData.postValue(Event(e))
    }

    protected fun postMessage(message: OctoActivity.Message) {
        mutableMessages.postValue(Event(message))
    }
}