package de.crysxd.octoapp.base.ui.widget.gcode

import android.content.Context
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.ui.common.gcodeshortcut.GcodeShortcutLayoutManager
import de.crysxd.octoapp.base.ui.ext.suspendedInflate
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import kotlinx.android.synthetic.main.widget_gcode.*
import kotlinx.android.synthetic.main.widget_gcode.view.*
import kotlinx.android.synthetic.main.widget_gcode_tutorial.*

class SendGcodeWidget(parent: Fragment) : OctoWidget(parent) {

    val viewModel: SendGcodeWidgetViewModel by injectViewModel()
    private lateinit var layoutManager: GcodeShortcutLayoutManager

    override fun getTitle(context: Context) = context.getString(R.string.widget_gcode_send)
    override fun getAnalyticsName() = "gcode"

    override suspend fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.suspendedInflate(R.layout.widget_gcode, container, false)

    override fun onViewCreated(view: View) {
        layoutManager = GcodeShortcutLayoutManager(
            layout = gcodeList,
            onClicked = { viewModel.sendGcodeCommand(it.command) },
            childFragmentManager = parent.childFragmentManager
        )
        view.buttonOpenTerminal.setOnClickListener {
            recordInteraction()
            it.findNavController().navigate(R.id.action_open_terminal)
        }

        if (!viewModel.isTutorialHidden()) {
            tutorialStub.inflate()
            buttonHideTutorial.setOnClickListener {
                viewModel.markTutorialHidden()
                TransitionManager.beginDelayedTransition(parent.requireActivity().window.decorView as ViewGroup)
                tutorial.isVisible = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.gcodes.observe(parent.viewLifecycleOwner, ::showGcodes)
    }

    private fun showGcodes(gcodes: List<GcodeHistoryItem>) {
        TransitionManager.beginDelayedTransition(gcodeList)
        layoutManager.showGcodes(gcodes.reversed())
    }
}