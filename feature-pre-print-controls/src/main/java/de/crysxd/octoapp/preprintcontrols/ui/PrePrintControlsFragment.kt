package de.crysxd.octoapp.preprintcontrols.ui

import android.os.Bundle
import android.view.View
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.menu.MenuBottomSheetFragment
import de.crysxd.baseui.menu.temperature.TemperatureMenu
import de.crysxd.baseui.widget.WidgetHostFragment
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.data.models.WidgetType
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.preprintcontrols.R
import de.crysxd.octoapp.preprintcontrols.di.injectViewModel

class PrePrintControlsFragment : WidgetHostFragment() {

    override val viewModel: PrePrintControlsViewModel by injectViewModel()
    override val destinationId = "preprint"
    override val toolbarState = OctoToolbar.State.Prepare

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.webCamSupported.observe(viewLifecycleOwner) { reloadWidgets("webcam-support-change") }
        bottomToolbar.setMainAction(R.string.start_printing).setOnClickListener { UriLibrary.getFileManagerUri().open(requireOctoActivity()) }
        bottomToolbar.menuButton.setOnClickListener { MenuBottomSheetFragment().show(childFragmentManager) }
        bottomToolbar.addAction(
            icon = R.drawable.ic_round_local_fire_department_24,
            title = R.string.temperature_menu___title,
            needsSwipe = false,
        ) {
            MenuBottomSheetFragment.createForMenu(TemperatureMenu()).show(childFragmentManager)
        }
    }

    override fun doReloadWidgets() {
        val webcamSupported = viewModel.webCamSupported.value == true
        val widgets = mutableListOf(
            WidgetType.AnnouncementWidget,
            WidgetType.ControlTemperatureWidget,
            WidgetType.MoveToolWidget,
            WidgetType.WebcamWidget,
            WidgetType.PrePrintQuickAccessWidget,
            WidgetType.SendGcodeWidget,
            WidgetType.ExtrudeWidget,
        ).also {
            if (!webcamSupported) {
                it.remove(WidgetType.WebcamWidget)
            }
        }

        installWidgets(widgets)
    }
}