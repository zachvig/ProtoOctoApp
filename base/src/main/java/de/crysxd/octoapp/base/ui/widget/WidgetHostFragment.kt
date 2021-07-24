package de.crysxd.octoapp.base.ui.widget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.WidgetHostFragmentBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.WidgetClass
import de.crysxd.octoapp.base.models.WidgetList
import de.crysxd.octoapp.base.models.WidgetPreferences
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.utils.InstantAutoTransition
import timber.log.Timber

abstract class WidgetHostFragment() : BaseWidgetHostFragment() {

    private lateinit var binding: WidgetHostFragmentBinding
    protected val mainButton get() = binding.mainButton
    protected val moreButton get() = binding.buttonMore
    abstract val destinationId: String
    abstract val toolbarState: OctoToolbar.State
    private var lastWidgetList: List<WidgetClass> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        WidgetHostFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("Create view")
        binding.widgetList.connectToLifecycle(viewLifecycleOwner)
    }

    override fun onStart() {
        super.onStart()
        Timber.i("Starting")
        requireOctoActivity().octoToolbar.state = toolbarState
        requireOctoActivity().octo.isVisible = true
        binding.widgetListScroller.setupWithToolbar(requireOctoActivity(), binding.bottomAction)
        reloadWidgets()
    }

    @CallSuper
    override fun reloadWidgets() {
        Timber.i("Reload widgets")
        requestTransition()
    }

    override fun requestTransition(quickTransition: Boolean) {
        TransitionManager.beginDelayedTransition(
            view as ViewGroup,
            if (quickTransition) InstantAutoTransition() else AutoTransition()
        )
    }

    fun installWidgets(list: List<WidgetClass>) {
        lastWidgetList = list
        internalInstallWidgets(list)
    }

    private fun internalInstallWidgets(list: List<WidgetClass>) {
        val order = Injector.get().widgetPreferencesRepository().getWidgetOrder(destinationId) ?: WidgetPreferences(destinationId, emptyList())
        val widgets = order.prepare(list).filter { !it.value }

        Timber.i("Installing widgets: $list")
        binding.widgetList.showWidgets(
            parent = this,
            widgetClasses = widgets
        )
    }

    fun startEdit() {
        val list = WidgetList()
        list.addAll(lastWidgetList)
        findNavController().navigate(R.id.action_edit_widgets, EditWidgetsFragmentArgs(destinationId, list).toBundle())
    }
}