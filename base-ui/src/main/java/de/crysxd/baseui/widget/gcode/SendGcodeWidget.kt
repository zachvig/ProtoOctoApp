package de.crysxd.baseui.widget.gcode

import android.content.Context
import android.view.LayoutInflater
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.findNavController
import de.crysxd.baseui.R
import de.crysxd.baseui.common.gcodeshortcut.GcodeShortcutLayoutManager
import de.crysxd.baseui.databinding.SendGcodeWidgetBinding
import de.crysxd.baseui.di.injectViewModel
import de.crysxd.baseui.widget.BaseWidgetHostFragment
import de.crysxd.baseui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.data.models.GcodeHistoryItem
import de.crysxd.octoapp.base.data.models.WidgetType

class SendGcodeWidget(context: Context) : RecyclableOctoWidget<SendGcodeWidgetBinding, SendGcodeWidgetViewModel>(context) {
    override val type = WidgetType.SendGcodeWidget
    override val binding = SendGcodeWidgetBinding.inflate(LayoutInflater.from(context))
    private var layoutManager: GcodeShortcutLayoutManager = GcodeShortcutLayoutManager(
        layout = binding.gcodeList,
        onClicked = { baseViewModel.sendGcodeCommand(it.command) },
    )

    init {
        binding.buttonOpenTerminal.setOnClickListener {
            recordInteraction()
            it.findNavController().navigate(R.id.action_open_terminal)
        }
    }

    override fun createNewViewModel(parent: BaseWidgetHostFragment) = parent.injectViewModel<SendGcodeWidgetViewModel>().value
    override fun getTitle(context: Context) = context.getString(R.string.widget_gcode_send)
    override fun getAnalyticsName() = "gcode"

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        baseViewModel.gcodes.observe(lifecycleOwner, ::showGcodes)
    }

    private fun showGcodes(gcodes: List<GcodeHistoryItem>) {
        parent.requestTransition()
        layoutManager.showGcodes(gcodes.reversed())
    }
}