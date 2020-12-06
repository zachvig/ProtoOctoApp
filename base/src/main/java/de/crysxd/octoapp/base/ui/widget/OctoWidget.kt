package de.crysxd.octoapp.base.ui.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import kotlinx.android.extensions.LayoutContainer

abstract class OctoWidget(val parent: Fragment) : LayoutContainer {

    protected lateinit var view: View

    override val containerView: View
        get() = view

    abstract fun getTitle(context: Context): String?

    abstract fun getAnalyticsName(): String

    abstract suspend fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View

    abstract fun onViewCreated(view: View)

    suspend fun getView(context: Context, container: ViewGroup): View {
        return if (!::view.isInitialized) {
            view = onCreateView(LayoutInflater.from(context), container)
            onViewCreated(view)
            view
        } else {
            view
        }
    }

    fun setupBaseViewModel(baseViewModel: BaseViewModel) {
        if (parent is BaseFragment) {
            parent.requireOctoActivity().observeErrorEvents(baseViewModel.errorLiveData)
            parent.requireOctoActivity().observerMessageEvents(baseViewModel.messages)
            baseViewModel.navContoller = parent.findNavController()
        }
    }

    fun requireContext() = parent.requireContext()

    protected fun recordInteraction() {
        OctoAnalytics.logEvent(OctoAnalytics.Event.WidgetInteraction(getAnalyticsName()))
    }
}