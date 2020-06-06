package de.crysxd.octoapp.base.ui

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineExceptionHandler
import timber.log.Timber

abstract class BaseViewModel : ViewModel() {

    private val mutableErrorLiveData = MutableLiveData<Throwable>()
    val errorLiveData = Transformations.map(mutableErrorLiveData) { it }
    protected val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        postException(e)
        Timber.e(e)
    }

    private val mutableMessages = MutableLiveData<(Context) -> CharSequence>()
    val messages = Transformations.map(mutableMessages) { it }

    lateinit var navContoller: NavController

    protected fun postException(e: Throwable) {
        mutableErrorLiveData.postValue(e)
    }

    protected fun postMessage(generator: (Context) -> CharSequence) {
        mutableMessages.postValue(generator)
    }


}