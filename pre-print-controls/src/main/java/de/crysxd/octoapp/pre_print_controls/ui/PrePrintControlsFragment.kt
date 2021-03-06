package de.crysxd.octoapp.pre_print_controls.ui

import android.os.Bundle
import android.view.View
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.widget.WidgetHostFragment
import de.crysxd.octoapp.base.ui.widget.announcement.AnnouncementWidget
import de.crysxd.octoapp.base.ui.widget.extrude.ExtrudeWidget
import de.crysxd.octoapp.base.ui.widget.gcode.SendGcodeWidget
import de.crysxd.octoapp.base.ui.widget.quickaccess.PrePrintQuickAccessWidget
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidget
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import de.crysxd.octoapp.pre_print_controls.ui.widget.move.MoveToolWidget

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

    override fun reloadWidgets() {
        super.reloadWidgets()
        val webcamSupported = viewModel.webCamSupported.value == true
        val widgets = mutableListOf(
            AnnouncementWidget::class,
            ControlTemperatureWidget::class,
            MoveToolWidget::class,
            WebcamWidget::class,
            PrePrintQuickAccessWidget::class,
            SendGcodeWidget::class,
            ExtrudeWidget::class,
        ).also {
            if (!webcamSupported) {
                it.remove(WebcamWidget::class)
            }
        }

        installWidgets(widgets)
    }
}