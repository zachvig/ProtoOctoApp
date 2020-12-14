package de.crysxd.octoapp.base.ui.widget.gcode

import android.content.Context
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.ui.ext.suspendedInflate
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import kotlinx.android.synthetic.main.widget_gcode.*
import kotlinx.android.synthetic.main.widget_gcode.view.*

class SendGcodeWidget(parent: Fragment) : OctoWidget(parent) {

    val viewModel: SendGcodeWidgetViewModel by injectViewModel()

    override fun getTitle(context: Context) = "Send Gcode"
    override fun getAnalyticsName() = "gcode"

    override suspend fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.suspendedInflate(R.layout.widget_gcode, container, false)

    override fun onViewCreated(view: View) {
        view.buttonOpenTerminal.setOnClickListener {
            recordInteraction()
            it.findNavController().navigate(R.id.action_open_terminal)
        }

        viewModel.gcodes.observe(parent, Observer(this::showGcodes))
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateGcodes()
    }

    private fun showGcodes(gcodes: List<GcodeHistoryItem>) {
        TransitionManager.beginDelayedTransition(gcodeList)

        // Remove all old views
        val removedViews = mutableListOf<Button>()
        gcodeList.children.toList().forEach {
            if (it.id != R.id.buttonOpenTerminal) {
                gcodeList.removeView(it)
                removedViews.add(it as Button)
            }
        }

        // Add new views
        gcodes.reversed().forEach { gcode ->
            val button = removedViews.firstOrNull { it.text.toString() == gcode.name }
                ?: LayoutInflater.from(requireContext()).inflate(R.layout.widget_gcode_button, gcodeList, false) as Button
            button.text = gcode.name
            gcodeList.addView(button, 0)
            button.setCompoundDrawablesRelativeWithIntrinsicBounds(
                if (gcode.isFavorite) {
                    R.drawable.ic_round_push_pin_16
                } else {
                    0
                }, 0, 0, 0
            )
            button.setOnClickListener {
                recordInteraction()
                viewModel.sendGcodeCommand(gcode.command)
            }
            button.setOnLongClickListener {
                viewModel.setFavorite(gcode, !gcode.isFavorite)
                true
            }
        }
    }
}