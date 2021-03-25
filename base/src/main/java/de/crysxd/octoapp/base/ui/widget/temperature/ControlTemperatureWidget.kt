package de.crysxd.octoapp.base.ui.widget.temperature

import android.content.Context
import android.view.LayoutInflater
import android.widget.GridLayout
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.TemperatureWidgetBinding
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.menu.temperature.TemperatureMenu
import de.crysxd.octoapp.base.ui.widget.BaseWidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.octoprint.models.socket.HistoricTemperatureData
import timber.log.Timber
import kotlin.math.absoluteValue

class ControlTemperatureWidget(context: Context) : RecyclableOctoWidget<TemperatureWidgetBinding, ControlTemperatureWidgetViewModel>(context) {

    override val binding = TemperatureWidgetBinding.inflate(LayoutInflater.from(context))
    private val observer = Observer(this::onTemperatureChanged)

    override fun createNewViewModel(parent: BaseWidgetHostFragment) = parent.injectViewModel<ControlTemperatureWidgetViewModel>().value

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        baseViewModel.temperature.observe(lifecycleOwner, observer)
    }

    override fun onPause() {
        super.onPause()
        baseViewModel.temperature.removeObserver(observer)
    }

    override fun getTitle(context: Context) = context.getString(R.string.widget_temperature)
    override fun getAnalyticsName(): String = "temperature"
    override fun getActionIcon() = R.drawable.ic_round_category_24
    override fun onAction() {
        MenuBottomSheetFragment.createForMenu(TemperatureMenu()).show(parent.childFragmentManager)
    }

    private fun onTemperatureChanged(data: List<HistoricTemperatureData>) {
        Timber.i("Temps:$data")
        if (data.isNotEmpty() && binding.root.childCount != data.last().components.size) {
            buildView(data.last().components.size)
        }

        data.lastOrNull()?.components?.let {
            it.keys.toList().forEachIndexed { index, key ->
                val view = binding.root.getChildAt(index) as TemperatureView
                view.setComponentName(baseViewModel.getComponentName(parent.requireContext(), key))
                view.maxTemp = baseViewModel.getMaxTemp(key)
                view.setTemperature(it[key])
                view.button.setOnClickListener {
                    baseViewModel.changeTemperature(parent.requireContext(), key)
                }
            }
        }
    }

    private fun buildView(count: Int) {
        parent.requestTransition()
        val change = count - binding.root.childCount
        val columns = binding.root.columnCount

        if (change < 0) {
            binding.root.children.take(change.absoluteValue).forEach {
                binding.root.removeView(it)
            }
        } else {
            repeat(change) {
                binding.root.addView(TemperatureView(parent.requireContext()))
            }
        }

        // Add bottom margin
        val margin = parent.requireContext().resources.getDimension(R.dimen.margin_0_1).toInt()
        val viewsInLastRow = binding.root.childCount % 2
        val fixedViewsInLastRow = if (viewsInLastRow == 0) 2 else viewsInLastRow
        val lastRowViews = binding.root.children.toList().takeLast(fixedViewsInLastRow)
        binding.root.children.forEach {
            val index = binding.root.indexOfChild(it)
            it.updateLayoutParams<GridLayout.LayoutParams> {
                columnSpec = GridLayout.spec(index % columns, 1f)
                if (index % columns == 0) {
                    marginEnd = margin
                } else {
                    marginStart = margin
                }

                bottomMargin = if (lastRowViews.contains(it)) 0 else margin * 2
            }
        }
    }
}