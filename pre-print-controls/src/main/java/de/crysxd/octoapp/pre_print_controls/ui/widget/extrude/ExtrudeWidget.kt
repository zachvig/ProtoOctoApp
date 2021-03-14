package de.crysxd.octoapp.pre_print_controls.ui.widget.extrude

import android.content.Context
import android.view.LayoutInflater
import de.crysxd.octoapp.base.ui.widget.BaseWidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.databinding.ExtrudeWidgetBinding
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel

class ExtrudeWidget(context: Context) : RecyclableOctoWidget<ExtrudeWidgetBinding, ExtrudeWidgetViewModel>(context) {

    override val binding = ExtrudeWidgetBinding.inflate(LayoutInflater.from(context))

    override fun getTitle(context: Context) = context.getString(R.string.widget_extrude)
    override fun getAnalyticsName() = "extrude"
    override fun createNewViewModel(parent: BaseWidgetHostFragment) = parent.injectViewModel<ExtrudeWidgetViewModel>().value

    init {
        binding.buttonExtrude5.setOnClickListener { recordInteraction(); baseViewModel.extrude5mm() }
        binding.buttonExtrude50.setOnClickListener { recordInteraction(); baseViewModel.extrude50mm() }
        binding.buttonExtrude100.setOnClickListener { recordInteraction(); baseViewModel.extrude100mm() }
        binding.buttonExtrude120.setOnClickListener { recordInteraction(); baseViewModel.extrude120mm() }
        binding.buttonExtrudeOther.setOnClickListener { recordInteraction(); baseViewModel.extrudeOther(context) }
    }
}