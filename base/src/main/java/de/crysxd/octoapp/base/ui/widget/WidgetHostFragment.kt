package de.crysxd.octoapp.base.ui.widget

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.WidgetHostFragmentBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.WidgetOrder
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import timber.log.Timber
import kotlin.reflect.KClass

abstract class WidgetHostFragment() : BaseFragment(R.layout.widget_host_fragment) {

    private lateinit var binding: WidgetHostFragmentBinding
    protected val mainButton get() = binding.mainButton
    protected val moreButton get() = binding.buttonMore
    abstract val destinationId: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("Create view")
        binding = WidgetHostFragmentBinding.bind(view)
        binding.widgetList.connectToLifecycle(viewLifecycleOwner)
        binding.widgetList.onWidgetOrderChanged = {
            Injector.get().widgetOrderRepository().setWidgetOrder(destinationId, WidgetOrder(destinationId, it))
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.i("Starting")
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
        binding.widgetListScroller.setupWithToolbar(requireOctoActivity(), binding.bottomAction)
    }

    @CallSuper
    open fun reloadWidgets() {
        Timber.i("Reload widgets")
    }

    fun requestTransition() {
        TransitionManager.beginDelayedTransition(view as ViewGroup)
    }

    fun installWidgets(list: List<KClass<out RecyclableOctoWidget<*, *>>>) {
        Timber.i("Installing widgets: $list")
        binding.widgetList.showWidgets(
            parent = this,
            widgetClasses = Injector.get().widgetOrderRepository().getWidgetOrder(destinationId)?.sort(list) ?: list
        )
    }
}