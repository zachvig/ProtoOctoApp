package de.crysxd.octoapp

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.transition.*
import com.google.firebase.analytics.FirebaseAnalytics
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.billing.BillingEvent
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.billing.PurchaseConfirmationDialog
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import de.crysxd.octoapp.pre_print_controls.di.Injector as ConnectPrinterInjector
import de.crysxd.octoapp.signin.di.Injector as SignInInjector

class MainActivity : OctoActivity() {

    private var lastNavigation = -1
    private val lastInsets = Rect()
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

            supportFragmentManager.findFragmentById(R.id.mainNavController)?.childFragmentManager?.registerFragmentLifecycleCallbacks(
                object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                        super.onFragmentResumed(fm, f)
                        applyInsetsToScreen(f)
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

                // For some odd reason root gets the paddings applied.....
                root.post { root.setPadding(0, 0, 0, 0) }

                insets.consumeStableInsets()
            }
        }
    }

    private fun findCurrentScreen() = supportFragmentManager.findFragmentById(R.id.mainNavController)?.childFragmentManager?.fragments?.firstOrNull()

    private fun applyInsetsToScreen(screen: Fragment, topOverwrite: Int? = null) {
        toolbar.updateLayoutParams<CoordinatorLayout.LayoutParams> { topMargin = topOverwrite ?: lastInsets.top }
        octo.updateLayoutParams<CoordinatorLayout.LayoutParams> { topMargin = topOverwrite ?: lastInsets.top }

        if (screen is InsetAwareScreen) {
            screen.handleInsets(
                Rect(
                    topOverwrite ?: lastInsets.top,
                    lastInsets.bottom,
                    lastInsets.left,
                    lastInsets.right
                )
            )
        } else {
            screen.view?.updatePadding(
                top = topOverwrite ?: lastInsets.top,
                bottom = lastInsets.bottom,
                left = lastInsets.left,
                right = lastInsets.right
            )
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
                delay(300L)
                it.consume { event ->
                    when (event) {
                        BillingEvent.PurchaseCompleted -> PurchaseConfirmationDialog().show(supportFragmentManager, "purchase-confirmation")
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
        is Event.Disconnected -> {
            Timber.w("Connection lost")
            when (e.exception) {
                is WebSocketMaybeBrokenException -> e.exception?.let(this::showDialog)
                else -> setDisconnectedMessageVisible(!listOf(R.id.action_connect_printer, R.id.action_sign_in_required).contains(lastNavigation))
            }
        }

        Event.Connected -> {
            Timber.w("Connection restored")
            setDisconnectedMessageVisible(false)
        }

        is Event.MessageReceived -> onMessageReceived(e.message)
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

                !flags.operational && !flags.paused && !flags.cancelling && !flags.printing && !flags.pausing -> {
                    stopService(notificationServiceIntent)
                    R.id.action_connect_printer
                }

                // Fallback
                else -> lastNavigation
            }
        )
    }

    private fun onEventMessageReceived(e: Message.EventMessage) = when (e) {
        is Message.EventMessage.Connected, is Message.EventMessage.SettingsUpdated -> {
            // New printer connected or settings updated, let's update capabilities
            updateCapabilities()
        }
        else -> Unit
    }

    private fun setDisconnectedMessageVisible(visible: Boolean) {
        // Let disconnect message fill status bar background
        disconnectedMessage.updatePadding(top = disconnectedMessage.paddingBottom + lastInsets.top)

        TransitionManager.beginDelayedTransition(root, TransitionSet().apply {
            addTransition(Explode())
            addTransition(ChangeBounds())
            addTransition(Fade())
            excludeChildren(coordinatorLayout, true)
        })

        // Use light status bar while disconnect message is shown
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            disconnectedMessage.systemUiVisibility = disconnectedMessage.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        findCurrentScreen()?.let { applyInsetsToScreen(it, 0.takeIf { visible }) }
        disconnectedMessage.isVisible = visible
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