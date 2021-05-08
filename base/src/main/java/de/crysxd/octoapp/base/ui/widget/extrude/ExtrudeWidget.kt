package de.crysxd.octoapp.base.ui.widget.extrude

import android.content.Context
import android.view.LayoutInflater
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.ExtrudeWidgetBinding
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.widget.BaseWidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import kotlinx.coroutines.flow.distinctUntilChanged

class ExtrudeWidget(context: Context) : RecyclableOctoWidget<ExtrudeWidgetBinding, ExtrudeWidgetViewModel>(context) {

    override val binding = ExtrudeWidgetBinding.inflate(LayoutInflater.from(context))

    override fun isVisible() = baseViewModel.isCurrentlyVisible
    override fun getTitle(context: Context) = context.getString(R.string.widget_extrude)
    override fun getAnalyticsName() = "extrude"
    override fun createNewViewModel(parent: BaseWidgetHostFragment) = parent.injectViewModel<ExtrudeWidgetViewModel>().value
    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        baseViewModel.isVisible.observe(lifecycleOwner) {
            parent.reloadWidgets()
        }
    }


    init {
        binding.buttonExtrude5.setOnClickListener { recordInteraction(); baseViewModel.extrude5mm() }
        binding.buttonExtrude50.setOnClickListener { recordInteraction(); baseViewModel.extrude50mm() }
        binding.buttonExtrude100.setOnClickListener { recordInteraction(); baseViewModel.extrude100mm() }
        binding.buttonExtrude120.setOnClickListener { recordInteraction(); baseViewModel.extrude120mm() }
        binding.buttonExtrudeOther.setOnClickListener { recordInteraction(); baseViewModel.extrudeOther(context) }
    }
}