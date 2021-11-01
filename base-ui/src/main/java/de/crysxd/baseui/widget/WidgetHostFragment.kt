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
import de.crysxd.baseui.widget.webcam.WebcamView
import de.crysxd.octoapp.base.data.models.WidgetPreferences
import de.crysxd.octoapp.base.data.models.WidgetType
import de.crysxd.octoapp.base.di.BaseInjector
import timber.log.Timber
import java.util.concurrent.TimeUnit

abstract class WidgetHostFragment() : BaseWidgetHostFragment() {

    companion object {
        private const val VIEW_ANIMATION_AFTER_CREATE_THRESHOLD = 300L
    }

    private lateinit var binding: WidgetHostFragmentBinding
    protected val mainButton get() = binding.mainButton
    protected val moreButton get() = binding.buttonMore
    abstract val destinationId: String
    abstract val toolbarState: OctoToolbar.State
    private var lastWidgetList: List<WidgetType> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private var viewCreatedAt = 0L
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
        postponeEnterTransition(VIEW_ANIMATION_AFTER_CREATE_THRESHOLD, TimeUnit.MILLISECONDS)
        viewCreatedAt = System.currentTimeMillis()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(reloadRunnable)
    }

    override fun onStart() {
        super.onStart()
        Timber.i("Starting")
        requireOctoActivity().octoToolbar.state = toolbarState
        requireOctoActivity().octo.isVisible = true
        binding.widgetListScroller.setupWithToolbar(requireOctoActivity(), binding.bottomAction)
        reloadWidgets("host-start")
    }

    @CallSuper
    override fun reloadWidgets(trigger: String) {
        Timber.i("Schedule reload widgets (trigger=$trigger)")
        handler.removeCallbacks(reloadRunnable)
        handler.postDelayed(reloadRunnable, 500)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(reloadRunnable)
    }

    abstract fun doReloadWidgets()

    override fun requestTransition(quickTransition: Boolean) {
        if ((System.currentTimeMillis() - viewCreatedAt) > VIEW_ANIMATION_AFTER_CREATE_THRESHOLD) {
            (view as? ViewGroup)?.let {
                // We need to exclude the Webcam view's children as this can cause animation glitches in the webcam view
                val transition = if (quickTransition) InstantAutoTransition() else AutoTransition()
                transition.excludeChildren(WebcamView::class.java, true)
                TransitionManager.beginDelayedTransition(it, transition)
            }
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
        startPostponedEnterTransition()
    }

    fun startEdit() {
        val list = WidgetList()
        list.addAll(lastWidgetList)
        findNavController().navigate(R.id.action_edit_widgets, EditWidgetsFragmentArgs(destinationId, list).toBundle())
    }
}