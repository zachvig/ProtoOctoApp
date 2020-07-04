package de.crysxd.octoapp.connect_printer.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.connect_printer.R
import de.crysxd.octoapp.connect_printer.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_connect_printer.*
import timber.log.Timber

class ConnectPrinterFragment : BaseFragment(R.layout.fragment_connect_printer) {

    override val viewModel: ConnectPrinterViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.uiState.observe(viewLifecycleOwner, Observer {
            Timber.i("$it")
            buttonTurnOnPsu.isVisible = false
            when (it) {
                ConnectPrinterViewModel.UiState.OctoPrintNotAvailable -> showStatus(
                    R.string.octoprint_is_not_available,
                    R.string.check_your_network_connection
                )

                ConnectPrinterViewModel.UiState.OctoPrintStarting -> showStatus(
                    R.string.octoprint_is_starting_up
                )

                ConnectPrinterViewModel.UiState.WaitingForPrinterToComeOnline -> {
                    buttonTurnOnPsu.isVisible = true
                    showStatus(
                        R.string.waiting_for_printer_to_come_online,
                        R.string.octoapp_will_auto_connect_the_printer_once_available
                    )
                }

                ConnectPrinterViewModel.UiState.PrinterConnecting -> showStatus(
                    R.string.printer_is_connecting
                )

                ConnectPrinterViewModel.UiState.Unknown -> showStatus(
                    R.string.error_general,
                    R.string.try_restrating_the_app_or_octoprint
                )
            }
        })

        buttonTurnOnPsu.setOnClickListener {
            viewModel.turnOnPsu()
        }
    }

    private fun showStatus(@StringRes state: Int, @StringRes subState: Int? = null) {
        textViewState.text = getString(state)
        textViewSubState.text = subState?.let(this::getString)
    }

    override fun onResume() {
        super.onResume()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Connect
        requireOctoActivity().octo.isVisible = false
    }

    override fun onPause() {
        super.onPause()
        requireOctoActivity().octo.isVisible = true
    }
}