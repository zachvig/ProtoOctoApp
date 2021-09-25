package de.crysxd.baseui.widget.temperature

import android.content.Context
import android.view.LayoutInflater
import android.widget.GridLayout
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import de.crysxd.baseui.R
import de.crysxd.baseui.databinding.TemperatureWidgetBinding
import de.crysxd.baseui.di.injectViewModel
import de.crysxd.baseui.menu.MenuBottomSheetFragment
import de.crysxd.baseui.menu.temperature.TemperatureMenu
import de.crysxd.baseui.widget.BaseWidgetHostFragment
import de.crysxd.baseui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.data.models.WidgetType
import de.crysxd.octoapp.base.data.repository.TemperatureDataRepository
import timber.log.Timber
import kotlin.math.absoluteValue

class ControlTemperatureWidget(context: Context) : RecyclableOctoWidget<TemperatureWidgetBinding, ControlTemperatureWidgetViewModel>(context) {
    override val type = WidgetType.ControlTemperatureWidget
    override val binding = TemperatureWidgetBinding.inflate(LayoutInflater.from(context))
    private val observer = Observer(this::onTemperatureChanged)

    override fun createNewViewModel(parent: BaseWidgetHostFragment) = parent.injectViewModel<ControlTemperatureWidgetViewModel>().value

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        baseViewModel.temperature.observe(lifecycleOwner, observer)
        buildView(baseViewModel.getInitialComponentCount())
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

    private fun onTemperatureChanged(data: List<TemperatureDataRepository.TemperatureSnapshot>) {
        if (data.isNotEmpty() && binding.root.childCount != data.size) {
            Timber.i("UI has ${binding.root.childCount} controls, but ${data.size} are needed. Rebuilding UI. ($data)")
            buildView(data.size)
        }

        data.forEachIndexed { index, it ->
            val view = binding.root.getChildAt(index) as TemperatureView
            view.setComponentName(baseViewModel.getComponentName(parent.requireContext(), it.component))
            view.maxTemp = baseViewModel.getMaxTemp(it.component)
            view.setTemperature(it)
            view.button.setOnClickListener { _ ->
                baseViewModel.changeTemperature(parent.requireContext(), it.component)
            }
        }
    }

    private fun buildView(count: Int) {
        Timber.i("Building UI for $count controls")
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