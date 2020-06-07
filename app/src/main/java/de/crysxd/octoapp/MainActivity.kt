package de.crysxd.octoapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import de.crysxd.octoapp.base.livedata.OctoTransformations.filter
import de.crysxd.octoapp.base.livedata.OctoTransformations.filterEventsForMessageType
import de.crysxd.octoapp.base.livedata.PollingLiveData
import de.crysxd.octoapp.base.usecase.CheckPrinterConnectedUseCase
import de.crysxd.octoapp.octoprint.models.printer.PrinterState
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.Exception
import de.crysxd.octoapp.pre_print_controls.di.Injector as ConnectPrinterInjector
import de.crysxd.octoapp.signin.di.Injector as SignInInjector

class MainActivity : AppCompatActivity() {

    private var lastNavigation = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val observer = Observer(this::onMessageReceived)
        val events = ConnectPrinterInjector.get().octoprintProvider().eventLiveData
            .filterEventsForMessageType(Message::class.java)

        SignInInjector.get().octoprintRepository().instanceInformation.observe(this, Observer {
            if (it != null) {
                events.observe(this, observer)
            } else {
                navigate(R.id.action_sign_in_required)
                events.removeObserver(observer)
            }
        })
    }

    private fun navigate(id: Int) {
        if (id != lastNavigation) {
            lastNavigation = id
            findNavController(R.id.mainNavController).navigate(id)
        }
    }

    private fun onMessageReceived(e: Message) {
        when (e) {
            is Message.CurrentMessage -> onCurrentMessageReceived(e)
            is Message.EventMessage -> onEventMessageReceived(e)
        }
    }

    private fun onCurrentMessageReceived(e: Message.CurrentMessage) {
        val flags = e.state?.flags
        navigate(
            when {
                flags == null || flags.closedOrError || flags.error -> R.id.action_connect_printer
                flags.printing || flags.paused || flags.pausing || flags.cancelling -> R.id.action_printer_active
                flags.operational -> R.id.action_printer_connected
                else -> lastNavigation
            }
        )
    }

    private fun onEventMessageReceived(e: Message.EventMessage) {
        navigate(
            when (e) {
                is Message.EventMessage.Disconnected -> R.id.action_connect_printer
                is Message.EventMessage.Connected,
                is Message.EventMessage.PrintCancelled,
                is Message.EventMessage.PrintFailed -> R.id.action_printer_connected
                is Message.EventMessage.PrintStarted -> R.id.action_printer_active
                else -> lastNavigation
            }
        )
    }
}