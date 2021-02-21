package de.crysxd.octoapp.connect_printer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.NetworkStateViewModel
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.menu.power.PowerControlsMenu
import de.crysxd.octoapp.connect_printer.R
import de.crysxd.octoapp.connect_printer.databinding.ConnectPrinterFragmentBinding
import de.crysxd.octoapp.connect_printer.di.injectViewModel
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.coroutines.delay
import timber.log.Timber

class ConnectPrinterFragment : BaseFragment(), PowerControlsMenu.PowerControlsCallback {

    private val networkViewModel: NetworkStateViewModel by injectViewModel(Injector.get().viewModelFactory())
    override val viewModel: ConnectPrinterViewModel by injectViewModel()
    private lateinit var binding: ConnectPrinterFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ConnectPrinterFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Subscribe to network state
        networkViewModel.networkState.observe(viewLifecycleOwner) {
            binding.noWifiWarning.isVisible = it !is NetworkStateViewModel.NetworkState.WifiConnected
        }

        // Subscribe to view state
        var lastState: ConnectPrinterViewModel.UiState? = null
        viewModel.uiState.observe(viewLifecycleOwner, { state ->
            Timber.i("$state")

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                val views = listOf(
                    binding.psuTurnOffControls,
                    binding.psuTurnOnControls,
                    binding.psuUnvailableControls,
                    binding.textViewState,
                    binding.textViewSubState
                )
                val duration = view.animate().duration
                if (lastState != state) {
                    views.forEach { it.animate().alpha(0f).start() }
                    delay(duration)
                    handleUiStateUpdate(state)
                    views.forEach { it.animate().alpha(1f).start() }
                }

                lastState = state
            }

            binding.buttonMore1.setOnClickListener { showMenu() }
            binding.buttonMore2.setOnClickListener { showMenu() }
            binding.buttonMore3.setOnClickListener { showMenu() }
            binding.buttonMore4.setOnClickListener { showMenu() }
            binding.buttonBeginConnect.setOnClickListener {
                requireOctoActivity().showDialog(
                    message = getString(R.string.connect_printer___begin_connection_confirmation_message),
                    positiveButton = getString(R.string.connect_printer___begin_connection_cofirmation_positive),
                    positiveAction = { viewModel.beginConnect() },
                    neutralButton = getString(R.string.connect_printer___begin_connection_cofirmation_negative)
                )
            }
        })
    }

    private fun showMenu() {
        MenuBottomSheetFragment().show(childFragmentManager)
    }

    private fun handleUiStateUpdate(state: ConnectPrinterViewModel.UiState) {
        binding.psuTurnOnControls.isVisible = false
        binding.psuTurnOffControls.isVisible = false

        when (state) {
            ConnectPrinterViewModel.UiState.OctoPrintNotAvailable -> {
                showStatus(
                    R.string.octoprint_is_not_available,
                    R.string.check_your_network_connection
                )
            }

            ConnectPrinterViewModel.UiState.OctoPrintStarting -> showStatus(
                R.string.octoprint_is_starting_up
            )

            is ConnectPrinterViewModel.UiState.WaitingForPrinterToComeOnline -> {
                binding.buttonTurnOnPsu.setOnClickListener {
                    MenuBottomSheetFragment.createForMenu(PowerControlsMenu(type = PowerControlsMenu.DeviceType.PrinterPsu, action = PowerControlsMenu.Action.TurnOn))
                        .show(childFragmentManager)
                }
                binding.buttonTurnOffPsu.setOnClickListener {
                    MenuBottomSheetFragment.createForMenu(PowerControlsMenu(type = PowerControlsMenu.DeviceType.PrinterPsu, action = PowerControlsMenu.Action.TurnOff))
                        .show(childFragmentManager)
                }
                binding.psuTurnOnControls.isVisible = state.psuIsOn == false
                binding.psuTurnOffControls.isVisible = state.psuIsOn == true
                binding.buttonTurnOnPsu.text = getString(R.string.turn_psu_on)
                binding.buttonTurnOffPsu.text = getString(R.string.turn_off_psu)
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
                binding.psuTurnOnControls.isVisible = true
                binding.buttonTurnOnPsu.setOnClickListener {
                    if (state.psuSupported) {
                        MenuBottomSheetFragment.createForMenu(
                            PowerControlsMenu(
                                type = PowerControlsMenu.DeviceType.PrinterPsu,
                                action = PowerControlsMenu.Action.Cycle
                            )
                        )
                            .show(childFragmentManager)
                    } else {
                        viewModel.retryConnectionFromOfflineState()
                    }
                }
                binding.buttonTurnOnPsu.setText(
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

            ConnectPrinterViewModel.UiState.WaitingForUser -> {
                binding.beginConnectControls.isVisible = true
                showStatus(
                    R.string.connect_printer___waiting_for_user_title,
                    R.string.connect_printer___waiting_for_user_subtitle
                )
            }

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

        binding.psuUnvailableControls.isVisible =
            !binding.psuTurnOnControls.isVisible && !binding.psuTurnOffControls.isVisible && !binding.beginConnectControls.isVisible

    }

    private fun showStatus(@StringRes state: Int, @StringRes subState: Int? = null) {
        binding.textViewState.text = getString(state)
        binding.textViewSubState.text = subState?.let(this::getString)
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Connect
        requireOctoActivity().octo.isVisible = false
    }

    override fun onPowerActionCompleted(action: PowerControlsMenu.Action, device: PowerDevice) {
        when (action) {
            PowerControlsMenu.Action.TurnOn -> viewModel.setDeviceOn(device, true)
            PowerControlsMenu.Action.TurnOff -> viewModel.setDeviceOn(device, false)
            PowerControlsMenu.Action.Cycle -> viewModel.cyclePsu(device)
            else -> Unit
        }
    }
}