package de.crysxd.octoapp.connect_printer.ui

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import de.crysxd.octoapp.base.PollingLiveData
import de.crysxd.octoapp.base.models.exceptions.NoPrinterConnectedException
import de.crysxd.octoapp.connect_printer.R
import de.crysxd.octoapp.connect_printer.di.injectViewModel
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintBootingException
import kotlinx.android.synthetic.main.fragment_connect_printer.*
import timber.log.Timber

class ConnectPrinterFragment : BaseFragment(R.layout.fragment_connect_printer) {

    override val viewModel: ConnectPrinterViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.printerState.observe(this, Observer {
            Timber.i("$it")
            when(it) {
                is PollingLiveData.Result.Success -> {
                    requireView().findNavController().navigate(R.id.action_printer_connected)
                }
                is PollingLiveData.Result.Failure -> {
                    val (state, subState) = when (it.exception) {
                        is OctoPrintBootingException -> Pair("OctoPrint is starting up", "")
                        is NoPrinterConnectedException -> Pair("Waiting for printer to auto-connect", "No printer is available")
                        else -> Pair("OctoPrint is not available", "Check your network connection")
                    }
                    textViewState.text = state
                    textViewSubState.text = subState
                    buttonTurnOnPsu.isVisible = it.exception is NoPrinterConnectedException
                    textViewAutoConnectInfo.isVisible = buttonTurnOnPsu.isVisible
                }
            }

            buttonTurnOnPsu.setOnClickListener {
                viewModel.turnOnPsu()
            }
        })
    }
}