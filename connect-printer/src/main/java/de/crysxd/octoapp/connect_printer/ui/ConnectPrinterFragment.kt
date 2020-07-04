package de.crysxd.octoapp.connect_printer.ui

import android.os.Bundle
import android.view.View
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
                    getString(R.string.octoprint_is_not_available),
                    getString(R.string.check_your_network_connection)
                )

                ConnectPrinterViewModel.UiState.OctoPrintStarting -> showStatus(
                    getString(R.string.octoprint_is_starting_up),
                    ""
                )

                ConnectPrinterViewModel.UiState.WaitingForPrinterToComeOnline -> {
                    buttonTurnOnPsu.isVisible = true
                    showStatus(
                        getString(R.string.waiting_for_printer_to_auto_connect),
                        getString(R.string.no_printer_is_available)
                    )
                }

                ConnectPrinterViewModel.UiState.PrinterConnecting -> showStatus(
                    "Printer is connecting…",
                    ""
                )

                ConnectPrinterViewModel.UiState.Unknown -> showStatus(
                    getString(R.string.error_general),
                    "Try restarting the app or OctoPrint"
                )
            }
        })

        buttonTurnOnPsu.setOnClickListener {
            viewModel.turnOnPsu()
        }
    }

    private fun showStatus(state: String, subState: String) {
        textViewState.text = state
        textViewSubState.text = subState
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