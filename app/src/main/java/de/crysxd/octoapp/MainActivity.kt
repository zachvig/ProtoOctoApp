package de.crysxd.octoapp

import android.os.Bundle
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import de.crysxd.octoapp.base.ui.OctoActivity
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.common.OctoView
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import de.crysxd.octoapp.pre_print_controls.di.Injector as ConnectPrinterInjector
import de.crysxd.octoapp.signin.di.Injector as SignInInjector

class MainActivity : OctoActivity() {

    private var lastNavigation = -1

    override val octoToolbar: OctoToolbar by lazy { toolbar }
    override val octo: OctoView by lazy { toolbarOctoView }
    override val coordinatorLayout: CoordinatorLayout by lazy { coordinator }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val observer = Observer(this::onEventReceived)
        val events = ConnectPrinterInjector.get().octoprintProvider().eventLiveData

        SignInInjector.get().octoprintRepository().instanceInformation.observe(this, Observer {
            Timber.i("Instance information received")
            if (it != null) {
                events.observe(this, observer)
            } else {
                navigate(R.id.action_sign_in_required)
                events.removeObserver(observer)
            }
        })

        lifecycleScope.launchWhenResumed {
            findNavController(R.id.mainNavController).addOnDestinationChangedListener { _, destination, _ ->
                Timber.i("Navigated to ${destination.label}")
                Firebase.analytics.setCurrentScreen(this@MainActivity, destination.label?.toString(), null)
            }
        }
    }

    private fun navigate(id: Int) {
        if (id != lastNavigation) {
            lastNavigation = id
            findNavController(R.id.mainNavController).navigate(id)
        }
    }

    private fun onEventReceived(e: Event) {
        when (e) {
            is Event.Disconnected -> navigate(R.id.action_connect_printer)
            is Event.MessageReceived -> onMessageReceived(e.message)
        }
    }

    private fun onMessageReceived(e: Message) {
        when (e) {
            is Message.CurrentMessage -> onCurrentMessageReceived(e)
            is Message.EventMessage -> onEventMessageReceived(e)
        }
    }

    private fun onCurrentMessageReceived(e: Message.CurrentMessage) {
        Timber.tag("navigation").v(e.state?.flags.toString())
        val flags = e.state?.flags
        navigate(
            when {
                // We encountered an error, try reconnecting
                flags == null || flags.closedOrError || flags.error -> R.id.action_connect_printer

                // We are printing
                flags.printing || flags.paused || flags.pausing || flags.cancelling -> R.id.action_printer_active

                // We are connected
                flags.operational -> R.id.action_printer_connected

                // This is a special case where all flags are false. This may happen after an emergency stop of the printer. Go to connect.
                !flags.operational && !flags.paused && !flags.cancelling && !flags.closedOrError && !flags.error && !flags.printing && !flags.pausing -> R.id.action_connect_printer

                // Fallback
                else -> lastNavigation
            }
        )
    }

    private fun onEventMessageReceived(e: Message.EventMessage) {
        Timber.tag("navigation").v(e.toString())
        navigate(
            when {
                e is Message.EventMessage.Disconnected -> R.id.action_connect_printer
                e is Message.EventMessage.Connected -> R.id.action_printer_connected
                e is Message.EventMessage.PrinterStateChanged &&
                        e.stateId == Message.EventMessage.PrinterStateChanged.PrinterState.OPERATIONAL -> R.id.action_printer_connected
                e is Message.EventMessage.PrintStarted -> R.id.action_printer_active
                else -> lastNavigation
            }
        )
    }
}