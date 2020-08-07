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
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import kotlinx.android.synthetic.main.widget_gcode.*
import kotlinx.android.synthetic.main.widget_gcode.view.*

class SendGcodeWidget(parent: Fragment) : OctoWidget(parent) {

    val viewModel: SendGcodeWidgetViewModel by injectViewModel()

    override fun getTitle(context: Context) = "Send Gcode"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.inflate(R.layout.widget_gcode, container, false)

    override fun onViewCreated(view: View) {
        (view as? ViewGroup)?.children?.filter {
            it is Button && it != view.buttonOpenTerminal
        }?.map {
            it as Button
        }?.forEach {
            it.setOnClickListener { _ -> viewModel.sendGcodeCommand(it.text.toString()) }
        }

        view.buttonOpenTerminal.setOnClickListener {
            viewModel.sendGcodeCommand(it.context)
        }

        viewModel.gcodes.observe(viewLifecycleOwner, Observer(this::showGcodes))
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
            val button = removedViews.firstOrNull { it.text.toString() == gcode.command }
                ?: LayoutInflater.from(requireContext()).inflate(R.layout.widget_gcode_button, gcodeList, false) as Button
            button.text = gcode.command
            gcodeList.addView(button, 0)
            button.setOnClickListener {
                viewModel.sendGcodeCommand(button.text.toString())
            }
        }
    }
}