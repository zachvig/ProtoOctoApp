package de.crysxd.octoapp.preprintcontrols.ui

import android.os.Bundle
import android.view.View
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.menu.MenuBottomSheetFragment
import de.crysxd.baseui.widget.WidgetHostFragment
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.data.models.WidgetType
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.preprintcontrols.di.injectViewModel

class PrePrintControlsFragment : WidgetHostFragment() {

    override val viewModel: PrePrintControlsViewModel by injectViewModel()
    override val destinationId = "preprint"
    override val toolbarState = OctoToolbar.State.Prepare

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainButton.setOnClickListener { UriLibrary.getFileManagerUri().open(requireOctoActivity()) }
        moreButton.setOnClickListener { MenuBottomSheetFragment().show(childFragmentManager) }
        viewModel.webCamSupported.observe(viewLifecycleOwner) { reloadWidgets() }
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