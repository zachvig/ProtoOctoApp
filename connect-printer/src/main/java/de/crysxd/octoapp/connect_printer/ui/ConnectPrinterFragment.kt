package de.crysxd.octoapp.connect_printer.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.transitionseverywhere.ChangeText
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.common.NetworkStateViewModel
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.base.ui.menu.power.PowerControlsMenu
import de.crysxd.octoapp.base.ui.menu.switchprinter.SwitchOctoPrintMenu
import de.crysxd.octoapp.connect_printer.R
import de.crysxd.octoapp.connect_printer.databinding.ConnectPrinterFragmentBinding
import de.crysxd.octoapp.connect_printer.di.injectViewModel
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import timber.log.Timber

class ConnectPrinterFragment : BaseFragment(), PowerControlsMenu.PowerControlsCallback {

    private val networkViewModel: NetworkStateViewModel by injectViewModel(Injector.get().viewModelFactory())
    override val viewModel: ConnectPrinterViewModel by injectViewModel()
    private lateinit var binding: ConnectPrinterFragmentBinding
    private var setDelayedStatusJob: Job? = null

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
                if (lastState != state) {
                    TransitionManager.beginDelayedTransition(binding.root, TransitionSet().also {
                        it.addTransition(Fade(Fade.OUT))
                        it.addTransition(Fade(Fade.IN).setStartDelay(300))
                        it.addTransition(ChangeBounds().apply {
                            excludeChildren(binding.root, true)
                            addTarget(binding.noWifiWarning)
                        })
                        it.addTransition(ChangeText().apply {
                            changeBehavior = ChangeText.CHANGE_BEHAVIOR_OUT_IN
                        })
                    })
                    handleUiStateUpdate(state)
                }

                lastState = state
            }

            binding.buttonMore1.setOnClickListener { showMenu() }
            binding.buttonMore2.setOnClickListener { showMenu() }
            binding.buttonMore3.setOnClickListener { showMenu() }
            binding.buttonMore4.setOnClickListener { showMenu() }
            binding.buttonMore5.setOnClickListener { showMenu() }
            binding.buttonTroubleShoot.setOnClickListener {
                viewModel.activeWebUrl?.let {
                    UriLibrary.getFixOctoPrintConnectionUri(baseUrl = Uri.parse(it), allowApiKeyResuse = true).open(requireOctoActivity())
                }
            }
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

    override fun onResume() {
        super.onResume()
        viewModel.uiState.value?.let(::handleUiStateUpdate)
    }

    private fun handleUiStateUpdate(state: ConnectPrinterViewModel.UiState) {
        binding.psuTurnOnControls.isVisible = false
        binding.psuTurnOffControls.isVisible = false
        binding.octoprintNotAvailableControls.isVisible = false
        binding.octoprintConnectedInfo.isVisible = !listOf(
            ConnectPrinterViewModel.UiState.Initializing,
            ConnectPrinterViewModel.UiState.OctoPrintNotAvailable,
            ConnectPrinterViewModel.UiState.OctoPrintStarting
        ).contains(state)
        binding.noWifiWarning.alpha = if (binding.octoprintConnectedInfo.isVisible) 0f else 1f
        binding.buttonChangeOctoPrint.setOnClickListener {
            MenuBottomSheetFragment.createForMenu(SwitchOctoPrintMenu()).show(childFragmentManager)
        }

        when (state) {
            ConnectPrinterViewModel.UiState.OctoPrintNotAvailable -> {
                binding.octoprintNotAvailableControls.isVisible = true
                showStatus(
                    R.string.connect_printer___octoprint_not_available_title,
                    R.string.connect_printer___octoprint_not_available_detail
                )
            }

            ConnectPrinterViewModel.UiState.OctoPrintStarting -> showStatus(
                R.string.connect_printer___octoprint_starting_title
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
                binding.buttonTurnOnPsu.text = getString(R.string.connect_printer___action_turn_psu_on)
                binding.buttonTurnOffPsu.text = getString(R.string.connect_printer___action_turn_psu_off)
                showStatus(
                    if (state.psuIsOn == true) {
                        R.string.connect_printer___psu_on_waiting_for_printer_title
                    } else {
                        R.string.connect_printer___waiting_for_printer_title
                    },
                    R.string.connect_printer___waiting_for_printer_detail
                )
            }

            ConnectPrinterViewModel.UiState.PrinterConnecting -> showStatus(
                R.string.connect_printer___printer_is_connecting_title
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
                        R.string.connect_printer___action_cycle_psu
                    } else {
                        R.string.connect_printer___action_retry
                    }
                )
                showStatus(
                    R.string.connect_printer___printer_offline_title,
                    if (state.psuSupported) {
                        R.string.connect_printer___printer_offline_detail_with_psu
                    } else {
                        R.string.connect_printer___printer_offline_detail
                    }
                )
            }

            is ConnectPrinterViewModel.UiState.PrinterPsuCycling -> showStatus(
                R.string.connect_printer___psu_cycling_title
            )

            ConnectPrinterViewModel.UiState.WaitingForUser -> {
                binding.beginConnectControls.isVisible = true
                showStatus(
                    R.string.connect_printer___waiting_for_user_title,
                    R.string.connect_printer___waiting_for_user_subtitle
                )
            }

            ConnectPrinterViewModel.UiState.Initializing -> showStatus(
                R.string.connect_printer___searching_for_octoprint_title
            )

            ConnectPrinterViewModel.UiState.PrinterConnected -> {
                showStatus(R.string.connect_printer___printer_connected_title, R.string.connect_printer___printer_connected_detail_1)
                showStatusDelayed(R.string.connect_printer___printer_connected_title, R.string.connect_printer___printer_connected_detail_2)
            }

            ConnectPrinterViewModel.UiState.Unknown -> showStatus(
                R.string.error_general,
                R.string.try_restrating_the_app_or_octoprint
            )
        }.let { }

        binding.psuUnvailableControls.isVisible = !binding.psuTurnOnControls.isVisible &&
                !binding.psuTurnOffControls.isVisible &&
                !binding.beginConnectControls.isVisible &&
                !binding.octoprintNotAvailableControls.isVisible

    }

    private fun showStatusDelayed(@StringRes state: Int, @StringRes subState: Int? = null) {
        setDelayedStatusJob = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            delay(10000)
            showStatus(state, subState)
        }
    }

    private fun showStatus(@StringRes state: Int, @StringRes subState: Int? = null) {
        setDelayedStatusJob?.cancel()
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
            PowerControlsMenu.Action.Toggle -> viewModel.setDeviceOn(
                device,
                (viewModel.uiState.value as? ConnectPrinterViewModel.UiState.WaitingForPrinterToComeOnline)?.psuIsOn?.not() ?: false
            )
            PowerControlsMenu.Action.Cycle -> viewModel.cyclePsu(device)
            else -> Unit
        }
    }
}