package de.crysxd.octoapp.connect_printer.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.connect_printer.R
import de.crysxd.octoapp.connect_printer.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_connect_printer.*
import kotlinx.coroutines.delay
import timber.log.Timber

class ConnectPrinterFragment : BaseFragment(R.layout.fragment_connect_printer) {

    override val viewModel: ConnectPrinterViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.uiState.observe(viewLifecycleOwner, Observer { state ->
            Timber.i("$state")

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                val views = stateViews.referencedIds.map { view.findViewById<View>(it) }
                val duration = view.animate().duration
                views.forEach { it.animate().alpha(0f).start() }
                delay(duration)
                handleUiStateUpdate(state)
                views.filter { it.isVisible }.forEach { it.animate().alpha(1f).start() }
            }
        })

        buttonTurnOnPsu.setOnClickListener {
            viewModel.togglePsu()
        }
    }

    private fun handleUiStateUpdate(state: ConnectPrinterViewModel.UiState) {
        buttonTurnOnPsu.isVisible = false

        when (state) {
            ConnectPrinterViewModel.UiState.OctoPrintNotAvailable -> showStatus(
                R.string.octoprint_is_not_available,
                R.string.check_your_network_connection
            )

            ConnectPrinterViewModel.UiState.OctoPrintStarting -> showStatus(
                R.string.octoprint_is_starting_up
            )

            is ConnectPrinterViewModel.UiState.WaitingForPrinterToComeOnline -> {
                buttonTurnOnPsu.isVisible = state.psuIsOn != null
                buttonTurnOnPsu.setText(
                    if (state.psuIsOn == true) {
                        R.string.turn_off_psu
                    } else {
                        R.string.turn_psu_on
                    }
                )
                showStatus(
                    if (state.psuIsOn == true) {
                        R.string.psu_turned_on_waiting_for_printer_to_boot
                    } else {
                        R.string.waiting_for_printer_to_come_online
                    },
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
    }

    private fun showStatus(@StringRes state: Int, @StringRes subState: Int? = null) {
        textViewState.text = getString(state)
        textViewSubState.text = subState?.let(this::getString)
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Connect
        requireOctoActivity().octo.isVisible = false
    }

    override fun onStop() {
        super.onStop()
        requireOctoActivity().octo.isVisible = true
    }
}