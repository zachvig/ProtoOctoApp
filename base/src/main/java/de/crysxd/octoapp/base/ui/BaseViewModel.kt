package de.crysxd.octoapp.base.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import timber.log.Timber

abstract class BaseViewModel : ViewModel() {

    private val mutableErrorLiveData = MutableLiveData<Throwable>()
    val errorLiveData = Transformations.map(mutableErrorLiveData) { it }
    protected val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        postException(e)
        Timber.e(e)
    }

    protected fun postException(e: Throwable) {
        mutableErrorLiveData.postValue(e)
    }


}