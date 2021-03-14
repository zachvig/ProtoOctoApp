package de.crysxd.octoapp.base.ui.widget

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
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
    var isEditMode
        get() = binding.widgetList.isInEditMode
        set(value) {
            requestTransition()
            binding.widgetList.isEditMode = value
            if (value) {
                layoutForEditView()
            } else {
                layoutForNormalView()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("Create view")
        binding = WidgetHostFragmentBinding.bind(view)
        binding.widgetList.connectToLifecycle(viewLifecycleOwner)
        binding.widgetList.onWidgetOrderChanged = {
            Injector.get().widgetOrderRepository().setWidgetOrder(destinationId, WidgetOrder(destinationId, it))
        }
        binding.finishEditMode.setOnClickListener { isEditMode = false }
    }

    override fun onStart() {
        super.onStart()
        Timber.i("Starting")
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
        binding.widgetListScroller.setupWithToolbar(requireOctoActivity(), binding.bottomAction)
    }

    private fun layoutForEditView() {
        binding.widgetListScroller.removeView(binding.widgetList)
        binding.root.addView(binding.widgetList)
        binding.widgetListScroller.updatePadding(bottom = requireContext().resources.getDimension(R.dimen.margin_6).toInt())
        binding.widgetListScroller.isVisible = false
        binding.bottomAction.isVisible = false
        binding.finishEditMode.isVisible = true
        requireOctoActivity().octoToolbar.isVisible = false
        requireOctoActivity().octo.isVisible = false
    }

    private fun layoutForNormalView() {
        binding.root.removeView(binding.widgetList)
        binding.widgetListScroller.addView(binding.widgetList)
        binding.widgetListScroller.updatePadding(bottom = 0)
        binding.widgetListScroller.isVisible = true
        binding.bottomAction.isVisible = true
        binding.finishEditMode.isVisible = false
        requireOctoActivity().octoToolbar.isVisible = true
        requireOctoActivity().octo.isVisible = true
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