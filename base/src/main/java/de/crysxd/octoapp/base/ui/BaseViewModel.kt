package de.crysxd.octoapp.base.ui

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.google.android.material.snackbar.Snackbar
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

    private val mutableMessages = MutableLiveData<Event<Message>>()
    val messages = Transformations.map(mutableMessages) { it }

    lateinit var navContoller: NavController

    protected fun postException(e: Throwable) {
        mutableErrorLiveData.postValue(Event(e))
    }

    protected fun postMessage(message: Message) {
        mutableMessages.postValue(Event(message))
    }

    sealed class Message {
        data class SnackbarMessage(
            val duration: Int = Snackbar.LENGTH_SHORT,
            val type: Type = Type.Neutral,
            val actionText: (Context) -> CharSequence? = { null },
            val action: (Context) -> Unit = {},
            val text: (Context) -> CharSequence
        ) : Message() {
            sealed class Type {
                object Neutral : Type()
                object Positive : Type()
                object Negative : Type()
            }
        }

        data class DialogMessage(
            val text: (Context) -> CharSequence,
            val positiveButton: (Context) -> CharSequence = { it.getString(android.R.string.ok) },
            val neutralButton: (Context) -> CharSequence? = { null },
            val positiveAction: (Context) -> Unit = {},
            val neutralAction: (Context) -> Unit = {}
        ) : Message()
    }

}