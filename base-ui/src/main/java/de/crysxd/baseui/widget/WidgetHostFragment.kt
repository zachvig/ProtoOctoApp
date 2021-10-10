package de.crysxd.baseui.widget

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import de.crysxd.baseui.R
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.databinding.WidgetHostFragmentBinding
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.utils.InstantAutoTransition
import de.crysxd.octoapp.base.data.models.WidgetPreferences
import de.crysxd.octoapp.base.data.models.WidgetType
import de.crysxd.octoapp.base.di.BaseInjector
import timber.log.Timber

abstract class WidgetHostFragment() : BaseWidgetHostFragment() {

    private lateinit var binding: WidgetHostFragmentBinding
    protected val mainButton get() = binding.mainButton
    protected val moreButton get() = binding.buttonMore
    abstract val destinationId: String
    abstract val toolbarState: OctoToolbar.State
    private var lastWidgetList: List<WidgetType> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private val reloadRunnable = Runnable {
        Timber.i("Reload widgets")
        requestTransition()
        doReloadWidgets()
    }

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
        Timber.i("Schedule reload widgets")
        handler.removeCallbacks(reloadRunnable)
        handler.postDelayed(reloadRunnable, 50)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(reloadRunnable)
    }

    abstract fun doReloadWidgets()

    override fun requestTransition(quickTransition: Boolean) {
        (view as? ViewGroup)?.let {
            TransitionManager.beginDelayedTransition(
                it,
                if (quickTransition) InstantAutoTransition() else AutoTransition()
            )
        }
    }

    fun installWidgets(list: List<WidgetType>) {
        lastWidgetList = list
        internalInstallWidgets(list)
    }

    private fun internalInstallWidgets(list: List<WidgetType>) {
        val order = BaseInjector.get().widgetPreferencesRepository().getWidgetOrder(destinationId) ?: WidgetPreferences(destinationId, emptyList())
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