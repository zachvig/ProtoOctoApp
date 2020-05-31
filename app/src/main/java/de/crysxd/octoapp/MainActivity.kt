package de.crysxd.octoapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import de.crysxd.octoapp.base.PollingLiveData
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import timber.log.Timber
import de.crysxd.octoapp.connect_printer.di.Injector as ConnectPrinterInjector
import de.crysxd.octoapp.signin.di.Injector as SignInInjector

class MainActivity : AppCompatActivity() {

    private var lastNavigation = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val printerState = ConnectPrinterInjector.get().octoprintProvider().printerState
        val observer = Observer(this::onPrinterStateChanged)

        SignInInjector.get().octoprintRepository().instanceInformation.observe(this, Observer {
            if (it != null) {
                navigate(R.id.action_sign_in_completed)
                printerState.observe(this, observer)
            } else {
                navigate(R.id.action_sign_in_required)
                printerState.removeObserver(observer)
            }
        })
    }

    private fun navigate(id: Int) {
        if (id != lastNavigation) {
            lastNavigation = id
            findNavController(R.id.mainNavController).navigate(id)
        }
    }

    private fun onPrinterStateChanged(printerState: PollingLiveData.Result<PrinterState>) {
        when (printerState) {
            is PollingLiveData.Result.Success -> {
                val f = printerState.t.state.flags
                when {
                    f.printing || f.cancelling || f.pausing || f.paused -> {
                        navigate(R.id.action_printer_active)
                    }
                    else -> {
                        navigate(R.id.action_printer_connected)
                    }
                }
            }

            is PollingLiveData.Result.Failure -> {
                Timber.e(printerState.exception)
                Timber.w("OctoPrint reported error, attempting to reconnect")
                navigate(R.id.action_connect_printer)
            }
        }.let {}
    }
}