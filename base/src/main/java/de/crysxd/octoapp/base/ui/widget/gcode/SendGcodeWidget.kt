package de.crysxd.octoapp.base.ui.widget.gcode

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.children
import androidx.fragment.app.Fragment
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.widget.OctoWidget
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
    }
}