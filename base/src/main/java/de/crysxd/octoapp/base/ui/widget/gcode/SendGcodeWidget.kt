package de.crysxd.octoapp.base.ui.widget.gcode

import android.content.Context
import android.view.LayoutInflater
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.findNavController
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.SendGcodeWidgetBinding
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.ui.common.gcodeshortcut.GcodeShortcutLayoutManager
import de.crysxd.octoapp.base.ui.widget.BaseWidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget

class SendGcodeWidget(context: Context) : RecyclableOctoWidget<SendGcodeWidgetBinding, SendGcodeWidgetViewModel>(context) {

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