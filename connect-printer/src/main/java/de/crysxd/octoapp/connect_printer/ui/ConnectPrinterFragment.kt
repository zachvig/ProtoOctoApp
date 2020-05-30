package de.crysxd.octoapp.connect_printer.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import de.crysxd.octoapp.connect_printer.R
import de.crysxd.octoapp.connect_printer.di.injectViewModel
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.BaseViewModel
import de.crysxd.octoapp.connect_printer.di.Injector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

class ConnectPrinterFragment : BaseFragment(R.layout.fragment_connect_printer) {

    override val viewModel: BaseViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Injector.get().octoprintRepository().octoprint.observe(this, Observer {
            GlobalScope.launch(Dispatchers.IO) {
                val state = it.createPrinterApi().getPrinterState()
                Timber.i("State: $state")
            }
        })
    }
}