package de.crysxd.octoapp

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.bundleOf
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.firebase.analytics.FirebaseAnalytics
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.billing.BillingEvent
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.InsetAwareScreen
import de.crysxd.octoapp.base.ui.OctoActivity
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.common.OctoView
import de.crysxd.octoapp.base.usecase.execute
import de.crysxd.octoapp.octoprint.exceptions.WebSocketMaybeBrokenException
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import de.crysxd.octoapp.pre_print_controls.di.Injector as ConnectPrinterInjector
import de.crysxd.octoapp.signin.di.Injector as SignInInjector

class MainActivity : OctoActivity() {

    private var lastNavigation = -1
    private val notificationServiceIntent by lazy { Intent(this, PrintNotificationService::class.java) }

    override val octoToolbar: OctoToolbar by lazy { toolbar }
    override val octo: OctoView by lazy { toolbarOctoView }
    override val coordinatorLayout: CoordinatorLayout by lazy { coordinator }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val observer = Observer(this::onEventReceived)
        val events = ConnectPrinterInjector.get().octoprintProvider().eventFlow("MainActivity@events").asLiveData()

        SignInInjector.get().octoprintRepository().instanceInformationFlow().asLiveData().observe(this, {
            Timber.i("Instance information received")
            if (it != null) {
                if (lastNavigation < 0) {
                    navigate(R.id.action_connect_printer)
                }
                events.observe(this, observer)
            } else {
                navigate(R.id.action_sign_in_required)
                events.removeObserver(observer)
            }
        })

        lifecycleScope.launchWhenResumed {
            findNavController(R.id.mainNavController).addOnDestinationChangedListener { _, destination, _ ->
                Timber.i("Navigated to ${destination.label}")
                OctoAnalytics.logEvent(OctoAnalytics.Event.ScreenShown, bundleOf(FirebaseAnalytics.Param.SCREEN_NAME to destination.label?.toString()))

                when (destination.id) {
                    R.id.loginFragment -> OctoAnalytics.logEvent(OctoAnalytics.Event.LoginWorkspaceShown)
                    R.id.connectPrinterFragment -> OctoAnalytics.logEvent(OctoAnalytics.Event.ConnectWorkspaceShown)
                    R.id.prePrintControlsFragment -> OctoAnalytics.logEvent(OctoAnalytics.Event.PrePrintWorkspaceShown)
                    R.id.printControlsFragment -> OctoAnalytics.logEvent(OctoAnalytics.Event.PrintWorkspaceShown)
                    R.id.terminalFragment -> OctoAnalytics.logEvent(OctoAnalytics.Event.TerminalWorkspaceShown)
                }
            }

            val lastInsets = Rect()
            supportFragmentManager.findFragmentById(R.id.mainNavController)?.childFragmentManager?.registerFragmentLifecycleCallbacks(
                object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                        super.onFragmentResumed(fm, f)
                        if (f is InsetAwareScreen) {
                            f.handleInsets(lastInsets)
                        } else {
                            f.view?.updatePadding(
                                top = lastInsets.top,
                                bottom = lastInsets.bottom,
                                left = lastInsets.left,
                                right = lastInsets.right
                            )
                        }
                    }
                },
                false
            )

            // Listen for inset changes and store them
            window.decorView.setOnApplyWindowInsetsListener { _, insets ->
                Timber.i("Insets updated $insets")
                lastInsets.top = insets.stableInsetTop
                lastInsets.left = insets.stableInsetLeft
                lastInsets.bottom = insets.stableInsetBottom
                lastInsets.right = insets.stableInsetRight
                toolbar.updateLayoutParams<CoordinatorLayout.LayoutParams> { topMargin = lastInsets.top }
                octo.updateLayoutParams<CoordinatorLayout.LayoutParams> { topMargin = lastInsets.top }
                insets.consumeStableInsets()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.i("UI started")
    }

    override fun onStop() {
        super.onStop()
        Timber.i("UI stopped")
    }

    override fun onResume() {
        super.onResume()
        BillingManager.onResume()
        lifecycleScope.launchWhenResumed {
            BillingManager.billingEventFlow().collectLatest {
                it.consume { event ->
                    when (event) {
                        BillingEvent.PurchaseCompleted -> showDialog(message = "Thanks for supporting OctoApp! You rock!", positiveButton = "Ok")
                    }
                }
            }
        }
    }

    private fun navigate(id: Int) {
        if (id != lastNavigation) {
            lastNavigation = id
            findNavController(R.id.mainNavController).navigate(id)
        }
    }

    private fun onEventReceived(e: Event) = when (e) {
        // Only show errors if we are not already in disconnected screen. We still want to show the stall warning to indicate something is wrong
        // as this might lead to the user being stuck
        is Event.Disconnected -> if (lastNavigation != R.id.action_connect_printer || e.exception is WebSocketMaybeBrokenException) {
            e.exception?.let(this::showDialog)
            navigate(R.id.action_connect_printer)
        } else {
            // We are already in disconnect state, nothing to do
            Timber.w("Socket disconnected but already in disconnected state (${e.exception}")
        }
        is Event.MessageReceived -> onMessageReceived(e.message)
        Event.Connected -> Unit
    }

    private fun onMessageReceived(e: Message) = when (e) {
        is Message.CurrentMessage -> onCurrentMessageReceived(e)
        is Message.EventMessage -> onEventMessageReceived(e)
        is Message.ConnectedMessage -> {
            // We are connected, let's update the available capabilities of the connect Octoprint
            updateCapabilities()
        }
        else -> Unit
    }

    private fun onCurrentMessageReceived(e: Message.CurrentMessage) {
        Timber.tag("navigation").v(e.state?.flags.toString())
        val flags = e.state?.flags
        navigate(
            when {
                // We encountered an error, try reconnecting
                flags == null || flags.closedOrError || flags.error -> {
                    stopService(notificationServiceIntent)
                    R.id.action_connect_printer
                }

                // We are printing
                flags.printing || flags.paused || flags.pausing || flags.cancelling -> {
                    try {
                        startService(notificationServiceIntent)
                    } catch (e: IllegalStateException) {
                        // User might have closed app just in time so we can't start the service
                    }
                    R.id.action_printer_active
                }

                // We are connected
                flags.operational -> {
                    stopService(notificationServiceIntent)
                    R.id.action_printer_connected
                }

                // This is a special case where all flags are false. This may happen after an emergency stop of the printer. Go to connect.
                !flags.operational && !flags.paused && !flags.cancelling && !flags.closedOrError && !flags.error && !flags.printing && !flags.pausing -> {
                    stopService(notificationServiceIntent)
                    R.id.action_connect_printer
                }

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
                e is Message.EventMessage.Connected -> {
                    // New printer connected, let's update capabilities
                    updateCapabilities()
                    R.id.action_printer_connected
                }
                e is Message.EventMessage.PrinterStateChanged &&
                        e.stateId == Message.EventMessage.PrinterStateChanged.PrinterState.OPERATIONAL -> R.id.action_printer_connected
                e is Message.EventMessage.PrintStarted -> R.id.action_printer_active
                e is Message.EventMessage.SettingsUpdated -> {
                    // Settings changed, let's update capabilities to see whether something changed
                    updateCapabilities()
                    lastNavigation
                }
                else -> lastNavigation
            }
        )
    }

    private fun updateCapabilities() {
        lifecycleScope.launchWhenCreated {
            try {
                Injector.get().updateInstanceCapabilitiesUseCase().execute()
            } catch (e: Exception) {
                Timber.e(e)
                showDialog(getString(R.string.capabilities_validation_error))
            }
        }
    }
}