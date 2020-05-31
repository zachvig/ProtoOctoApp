package de.crysxd.octoapp.connect_printer.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import de.crysxd.octoapp.base.PollingLiveData
import de.crysxd.octoapp.connect_printer.R
import de.crysxd.octoapp.connect_printer.di.injectViewModel
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.connect_printer.di.Injector
import kotlinx.android.synthetic.main.fragment_connect_printer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

class ConnectPrinterFragment : BaseFragment(R.layout.fragment_connect_printer) {

    override val viewModel: ConnectPrinterViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.printerState.observe(this, Observer {
            Timber.i("$it")
            if (it is PollingLiveData.Result.Success) {
                requireView().findNavController().navigate(R.id.action_printer_connected)
            }
        })
    }
}