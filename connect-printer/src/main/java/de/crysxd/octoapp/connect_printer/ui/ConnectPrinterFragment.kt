package de.crysxd.octoapp.connect_printer.ui

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.livedata.PollingLiveData
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

        textViewAutoConnectInfo.movementMethod = LinkMovementMethod()
        textViewAutoConnectInfo.text = HtmlCompat.fromHtml(
            getString(R.string.install_then_port_listener_plugin_to_enable_auto_connect),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        viewModel.printerState.observe(viewLifecycleOwner, Observer {
            Timber.i("$it")
            when (it) {
                is PollingLiveData.Result.Success -> {
                    // MainActivity will navigate away
                }
                is PollingLiveData.Result.Failure -> {
                    val (state, subState) = when (it.exception) {
                        is OctoPrintBootingException -> Pair(getString(R.string.octoprint_is_starting_up), "")
                        is NoPrinterConnectedException -> Pair(getString(R.string.waiting_for_printer_to_auto_connect), getString(R.string.no_printer_is_available))
                        else -> Pair(getString(R.string.octoprint_is_not_available), getString(R.string.check_your_network_connection))
                    }
                    textViewState.text = state
                    textViewSubState.text = subState
                    buttonTurnOnPsu.isVisible = it.exception is NoPrinterConnectedException
                    textViewAutoConnectInfo.isVisible = buttonTurnOnPsu.isVisible
                }
            }.let { }

            buttonTurnOnPsu.setOnClickListener {
                viewModel.turnOnPsu()
            }
        })
    }
}