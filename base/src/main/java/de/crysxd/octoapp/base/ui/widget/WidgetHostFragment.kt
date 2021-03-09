package de.crysxd.octoapp.base.ui.widget

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.WidgetHostFragmentBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.WidgetOrder
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import kotlin.reflect.KClass

abstract class WidgetHostFragment() : BaseFragment(R.layout.widget_host_fragment) {

    private lateinit var binding: WidgetHostFragmentBinding
    protected val mainButton get() = binding.mainButton
    protected val moreButton get() = binding.buttonMore
    abstract val destinationId: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = WidgetHostFragmentBinding.bind(view)
        binding.widgetList.connectToLifecycle(viewLifecycleOwner)
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
        binding.widgetListScroller.setupWithToolbar(requireOctoActivity(), binding.bottomAction)
        binding.widgetList.onWidgetOrderChanged = {
            Injector.get().widgetOrderRepository().setWidgetOrder(destinationId, WidgetOrder(destinationId, it))
        }
    }

    abstract fun reloadWidgets()

    fun requestTransition() {
        TransitionManager.beginDelayedTransition(view as ViewGroup)
    }

    fun installWidgets(list: List<KClass<out RecyclableOctoWidget<*, *>>>) {
        binding.widgetList.showWidgets(
            parent = this,
            widgetClasses = Injector.get().widgetOrderRepository().getWidgetOrder(destinationId)?.sort(list) ?: list
        )
    }
}