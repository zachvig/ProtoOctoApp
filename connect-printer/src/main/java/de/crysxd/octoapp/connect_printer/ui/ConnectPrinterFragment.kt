package de.crysxd.octoapp.connect_printer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.transition.TransitionManager
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.ext.suspendedInflate
import de.crysxd.octoapp.connect_printer.R
import de.crysxd.octoapp.connect_printer.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_connect_printer.*
import kotlinx.coroutines.delay
import timber.log.Timber

class ConnectPrinterFragment : BaseFragment() {

    override val viewModel: ConnectPrinterViewModel by injectViewModel()
    private var firstStateUpdate = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = FrameLayout(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenCreated {
            // Async inflate view
            val lazyView = LayoutInflater.from(requireContext()).suspendedInflate(R.layout.fragment_connect_printer, view as ViewGroup, false)
            TransitionManager.beginDelayedTransition(view)
            view.addView(lazyView)

            // Subscribe to view state
            viewModel.uiState.observe(viewLifecycleOwner, Observer { state ->
                Timber.i("$state")

                viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                    val views = listOf(buttonTurnOnPsu, buttonTurnOffPsu, textViewState, textViewSubState)
                    val duration = view.animate().duration
                    if (firstStateUpdate) {
                        handleUiStateUpdate(state)
                        firstStateUpdate = false
                    } else {
                        views.forEach { it.animate().alpha(0f).start() }
                        delay(duration)
                        handleUiStateUpdate(state)
                        views.forEach { it.animate().alpha(1f).start() }
                    }
                }

                buttonOpenOctoprint.setOnClickListener {
                    viewModel.openWebInterface(it.context)
                }

                buttonSignOut.setOnClickListener {
                    viewModel.signOut()
                }
            })
        }
    }

    private fun handleUiStateUpdate(state: ConnectPrinterViewModel.UiState) {
        buttonTurnOnPsu.isVisible = false
        buttonTurnOffPsu.isVisible = false
        buttonOpenOctoprint.isVisible = false
        buttonSignOut.isVisible = false

        when (state) {
            ConnectPrinterViewModel.UiState.OctoPrintNotAvailable -> {
                buttonSignOut.isVisible = true
                showStatus(
                    R.string.octoprint_is_not_available,
                    R.string.check_your_network_connection
                )
            }

            ConnectPrinterViewModel.UiState.OctoPrintStarting -> showStatus(
                R.string.octoprint_is_starting_up
            )

            is ConnectPrinterViewModel.UiState.WaitingForPrinterToComeOnline -> {
                buttonTurnOnPsu.setOnClickListener {
                    viewModel.togglePsu()
                }
                buttonTurnOffPsu.setOnClickListener {
                    viewModel.togglePsu()
                }
                buttonTurnOnPsu.isVisible = state.psuIsOn == false
                buttonTurnOffPsu.isVisible = state.psuIsOn == true
                buttonTurnOnPsu.text = getString(R.string.turn_psu_on)
                buttonTurnOffPsu.text = getString(R.string.turn_off_psu)
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

            is ConnectPrinterViewModel.UiState.PrinterOffline -> {
                buttonOpenOctoprint.isVisible = true
                buttonTurnOnPsu.isVisible = true
                buttonTurnOnPsu.setOnClickListener {
                    if (state.psuSupported) {
                        viewModel.cyclePsu()
                    } else {
                        viewModel.retryConnectionFromOfflineState()
                    }
                }
                buttonTurnOnPsu.setText(
                    if (state.psuSupported) {
                        R.string.cycle_psu
                    } else {
                        R.string.retry_connection
                    }
                )
                showStatus(
                    R.string.printer_is_offline,
                    if (state.psuSupported) {
                        R.string.cycle_psu_to_reset_the_printer
                    } else {
                        R.string.turn_the_printer_off_and_on_again_to_reset_it
                    }
                )
            }

            is ConnectPrinterViewModel.UiState.PrinterPsuCycling -> showStatus(
                R.string.psu_is_being_cycled
            )

            ConnectPrinterViewModel.UiState.Initializing -> showStatus(
                R.string.searching_for_octoprint
            )

            ConnectPrinterViewModel.UiState.PrinterConnected -> showStatus(
                R.string.printer_connected
            )

            ConnectPrinterViewModel.UiState.Unknown -> showStatus(
                R.string.error_general,
                R.string.try_restrating_the_app_or_octoprint
            )
        }.let { }
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
}