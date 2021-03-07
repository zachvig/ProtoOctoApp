package de.crysxd.octoapp.base.ui.widget.temperature

import android.content.Context
import android.view.LayoutInflater
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.TemperatureWidgetBinding
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.menu.temperature.TemperatureMenu
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.ui.widget.WidgetHostFragment
import de.crysxd.octoapp.octoprint.models.printer.PrinterState

class ControlTemperatureWidget(context: Context) : RecyclableOctoWidget<TemperatureWidgetBinding, BaseViewModel>(context) {

    override val binding = TemperatureWidgetBinding.inflate(LayoutInflater.from(context))
    private lateinit var toolViewModel: ControlToolTemperatureWidgetViewModel
    private lateinit var bedViewModel: ControlBedTemperatureWidgetViewModel

    override fun createNewViewModel(parent: WidgetHostFragment): BaseViewModel? {
        toolViewModel = parent.injectViewModel<ControlToolTemperatureWidgetViewModel>().value
        bedViewModel = parent.injectViewModel<ControlBedTemperatureWidgetViewModel>().value
        bedViewModel.temperature.observe(parent, Observer(this::onBedTemperatureChanged))
        toolViewModel.temperature.observe(parent, Observer(this::onToolTemperatureChanged))
        toolViewModel.navContoller = parent.findNavController()
        bedViewModel.navContoller = parent.findNavController()
        binding.bedTemperature.setComponentName(view.context.getString(bedViewModel.getComponentName()))
        binding.toolTemperature.setComponentName(view.context.getString(toolViewModel.getComponentName()))
        return null
    }

    override fun getTitle(context: Context) = context.getString(R.string.widget_temperature)
    override fun getAnalyticsName(): String = "temperature"
    override fun getActionIcon() = R.drawable.ic_round_category_24
    override fun onAction() {
        MenuBottomSheetFragment.createForMenu(TemperatureMenu()).show(parent.childFragmentManager)
    }

    init {
        binding.bedTemperature.button.setOnClickListener {
            recordInteraction()
            bedViewModel.changeTemperature(it.context)
        }
        binding.toolTemperature.button.setOnClickListener {
            recordInteraction()
            toolViewModel.changeTemperature(it.context)
        }
    }

    private fun onBedTemperatureChanged(temperature: PrinterState.ComponentTemperature) {
        binding.bedTemperature.setTemperature(temperature)
    }

    private fun onToolTemperatureChanged(temperature: PrinterState.ComponentTemperature) {
        binding.toolTemperature.setTemperature(temperature)
    }
}