package de.crysxd.octoapp.base.ui.widget.progress

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.print_controls.di.injectViewModel

class ProgressWidget(parent: Fragment) : OctoWidget(parent) {

    private val viewModel: ProgressWidgetViewModel by injectViewModel()

    override fun getTitle(context: Context) = "Prorgess"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
        return TextView(inflater.context)
    }

    override fun onViewCreated(view: View) {
        viewModel.printState.observe(viewLifecycleOwner, Observer {
            val textView = view as TextView
            textView.text = """
Status: ${formatStatus(it.state)}
Print time: ${it.progress?.printTime?.let(::formatDuration)}
Print time left: ${it.progress?.printTimeLeft?.let(::formatDuration)}
Estimation method: ${it.progress?.printTimeLeftOrigin}"""
        })
    }

    private fun formatDuration(seconds: Int): String = if (seconds < 60) {
        seconds.toString()
    } else {
        DateUtils.formatElapsedTime(seconds.toLong())
    }

    private fun formatStatus(state: PrinterState.State?) = when {
        state?.flags?.pausing == true -> "Pausing"
        state?.flags?.paused == true -> "Paused"
        state?.flags?.printing == true -> "Printing"
        state?.flags?.operational == true -> "Ready"
        state?.flags?.error == true -> "Error"
        state?.flags?.closedOrError == true -> "Error or closed"
        state?.flags?.cancelling == true -> "Cancelling"
        else -> "Unknown"
    }
}