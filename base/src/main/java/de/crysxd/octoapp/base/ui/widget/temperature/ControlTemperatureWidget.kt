package de.crysxd.octoapp.base.ui.widget.temperature

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.common.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.common.menu.temperature.TemperatureMenu
import de.crysxd.octoapp.base.ui.ext.suspendedInflate
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import kotlinx.android.synthetic.main.view_temperature.view.*
import kotlinx.android.synthetic.main.widget_temperature.view.*

class ControlTemperatureWidget(parent: Fragment) : OctoWidget(parent) {

    private val toolViewModel: ControlToolTemperatureWidgetViewModel by injectViewModel()
    private val bedViewModel: ControlBedTemperatureWidgetViewModel by injectViewModel()

    override fun getTitle(context: Context) = context.getString(R.string.widget_temperature)
    override fun getAnalyticsName(): String = "temperature"
    override fun getMoreIcon() = R.drawable.ic_round_category_24
    override fun showMore() {
        MenuBottomSheetFragment.createForMenu(TemperatureMenu()).show(parent.childFragmentManager)
        super.showMore()
    }

    override suspend fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.suspendedInflate(R.layout.widget_temperature, container, false)

    override fun onViewCreated(view: View) {
        bedViewModel.temperature.observe(parent, Observer(this::onBedTemperatureChanged))
        toolViewModel.temperature.observe(parent, Observer(this::onToolTemperatureChanged))

        view.bedTemperature.setComponentName(view.context.getString(bedViewModel.getComponentName()))
        view.bedTemperature.button.setOnClickListener {
            recordInteraction()
            bedViewModel.changeTemperature(it.context)
        }
        view.toolTemperature.setComponentName(view.context.getString(toolViewModel.getComponentName()))
        view.toolTemperature.button.setOnClickListener {
            recordInteraction()
            toolViewModel.changeTemperature(it.context)
        }
    }

    private fun onBedTemperatureChanged(temperature: PrinterState.ComponentTemperature) {
        view.bedTemperature.setTemperature(temperature)
    }

    private fun onToolTemperatureChanged(temperature: PrinterState.ComponentTemperature) {
        view.toolTemperature.setTemperature(temperature)
    }
}