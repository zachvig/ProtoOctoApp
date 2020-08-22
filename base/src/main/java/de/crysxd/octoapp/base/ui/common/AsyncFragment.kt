package de.crysxd.octoapp.base.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.transition.Fade
import androidx.transition.TransitionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import timber.log.Timber

abstract class AsyncFragment : Fragment() {

    private var viewCreationJob: Job? = null
    private val lazyContainer: ViewGroup by lazy { FrameLayout(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewCreationJob = lifecycleScope.launchWhenCreated {
            val view = try {
                withContext(Dispatchers.Default) {
                    onCreateViewAsync(inflater, container, savedInstanceState)
                }
            } catch (e: RuntimeException) {
                Timber.w("Failed lazy inflation, attempting on main thread")
                Timber.v(e)
                onCreateViewAsync(inflater, container, savedInstanceState)
            }

            TransitionManager.beginDelayedTransition(lazyContainer, onCreateLazyViewEntryTransition())
            lazyContainer.addView(view)
            onLazyViewCreated(view, savedInstanceState)
        }

        return lazyContainer
    }

    suspend fun awaitLazyView() = viewCreationJob?.join()

    open fun onCreateLazyViewEntryTransition() = Fade()
    abstract fun onLazyViewCreated(view: View, savedInstanceState: Bundle?)
    abstract fun onCreateViewAsync(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View

}