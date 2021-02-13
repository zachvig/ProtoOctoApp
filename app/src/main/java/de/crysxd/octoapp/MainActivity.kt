package de.crysxd.octoapp

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
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
import androidx.transition.ChangeBounds
import androidx.transition.Explode
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.firebase.analytics.FirebaseAnalytics
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.billing.BillingEvent
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.billing.PurchaseConfirmationDialog
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.ColorTheme
import de.crysxd.octoapp.base.ui.InsetAwareScreen
import de.crysxd.octoapp.base.ui.OctoActivity
import de.crysxd.octoapp.base.ui.colorTheme
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.common.OctoView
import de.crysxd.octoapp.base.usecase.UpdateInstanceCapabilitiesUseCase
import de.crysxd.octoapp.octoprint.exceptions.WebSocketMaybeBrokenException
import de.crysxd.octoapp.octoprint.exceptions.WebSocketUpgradeFailedException
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.widgets.updateAllWidgets
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import timber.log.Timber
import de.crysxd.octoapp.octoprint.models.socket.Message as SocketMessage
import de.crysxd.octoapp.pre_print_controls.di.Injector as ConnectPrinterInjector
import de.crysxd.octoapp.signin.di.Injector as SignInInjector

const val KEY_LAST_NAVIGATION = "lastNavigation"
const val EXTRA_TARGET_OCTOPRINT_WEB_URL = "octoprint_web_url"

class MainActivity : OctoActivity() {

    private var lastNavigation = -1
    private val lastInsets = Rect()
    private var lastSuccessfulCapabilitiesUpdate = 0L

    override val octoToolbar: OctoToolbar by lazy { toolbar }
    override val octo: OctoView by lazy { toolbarOctoView }
    override val coordinatorLayout: CoordinatorLayout by lazy { coordinator }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val observer = Observer(this::onEventReceived)
        val events = ConnectPrinterInjector.get().octoprintProvider().eventFlow("MainActivity@events").asLiveData()

        onNewIntent(intent)

        lastNavigation = savedInstanceState?.getInt(KEY_LAST_NAVIGATION, lastNavigation) ?: lastNavigation
        SignInInjector.get().octoprintRepository().instanceInformationFlow()
            .distinctUntilChangedBy { it?.webUrl }
            .asLiveData()
            .observe(this, {
                Timber.i("Instance information received")
                updateAllWidgets()
                if (it != null && it.apiKey.isNotBlank()) {
                    navigate(R.id.action_connect_printer)
                    events.observe(this, observer)
                } else {
                    navigate(R.id.action_sign_in_required)
                    PrintNotificationService.stop(this)
                    (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(PrintNotificationService.NOTIFICATION_ID)
                    events.removeObserver(observer)
                }
            })

        SignInInjector.get().octoprintRepository().instanceInformationFlow()
            .distinctUntilChangedBy { it?.settings?.appearance?.color }
            .asLiveData()
            .observe(this) { ColorTheme.applyColorTheme(it.colorTheme) }

        lifecycleScope.launchWhenResumed {
            findNavController(R.id.mainNavController).addOnDestinationChangedListener { _, destination, _ ->
                Timber.i("Navigated to ${destination.label}")
                OctoAnalytics.logEvent(OctoAnalytics.Event.ScreenShown, bundleOf(FirebaseAnalytics.Param.SCREEN_NAME to destination.label?.toString()))

                when (destination.id) {
                    R.id.loginFragment -> OctoAnalytics.logEvent(OctoAnalytics.Event.LoginWorkspaceShown)
                    R.id.workspaceConnect -> OctoAnalytics.logEvent(OctoAnalytics.Event.ConnectWorkspaceShown)
                    R.id.workspacePrePrint -> OctoAnalytics.logEvent(OctoAnalytics.Event.PrePrintWorkspaceShown)
                    R.id.workspacePrint -> OctoAnalytics.logEvent(OctoAnalytics.Event.PrintWorkspaceShown)
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
                lastInsets.top = insets.systemWindowInsetTop
                lastInsets.left = insets.systemWindowInsetLeft
                lastInsets.bottom = insets.systemWindowInsetBottom
                lastInsets.right = insets.systemWindowInsetRight
                applyInsetsToCurrentScreen()
                insets.consumeStableInsets()
            }
        }

        if (!isTablet()) {
            // Stop screen rotation on phones
            @SuppressLint("SourceLockedOrientationActivity")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        // Observe settings
        Injector.get().octoPreferences().updatedFlow.asLiveData().observe(this) {
            lifecycleScope.launchWhenCreated {
                Injector.get().applyLegacyDarkModeUseCase().execute(this@MainActivity)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getStringExtra(EXTRA_TARGET_OCTOPRINT_WEB_URL)?.let { webUrl ->
            val repo = Injector.get().octorPrintRepository()
            repo.getAll().firstOrNull { it.webUrl == webUrl }?.let {
                repo.setActive(it)
            }
        }
    }

    private fun isTablet() = ((this.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_LAST_NAVIGATION, lastNavigation)
    }

    private fun applyInsetsToCurrentScreen() = findCurrentScreen()?.let { applyInsetsToScreen(it) }

    private fun findCurrentScreen() = supportFragmentManager.findFragmentById(R.id.mainNavController)?.childFragmentManager?.fragments?.firstOrNull()

    private fun applyInsetsToScreen(screen: Fragment, topOverwrite: Int? = null) {
        val disconnectHeight = disconnectedMessage.height.takeIf { disconnectedMessage.isVisible }
        Timber.v("Applying insets: disconnectedMessage=$disconnectHeight topOverwrite=$topOverwrite")
        toolbar.updateLayoutParams<CoordinatorLayout.LayoutParams> { topMargin = topOverwrite ?: disconnectHeight ?: lastInsets.top }
        octo.updateLayoutParams<CoordinatorLayout.LayoutParams> { topMargin = topOverwrite ?: disconnectHeight ?: lastInsets.top }

        if (screen is InsetAwareScreen) {
            screen.handleInsets(
                Rect(
                    lastInsets.left,
                    topOverwrite ?: disconnectHeight ?: lastInsets.top,
                    lastInsets.right,
                    lastInsets.bottom,
                )
            )
        } else {
            screen.view?.updatePadding(
                top = topOverwrite ?: disconnectHeight ?: lastInsets.top,
                bottom = lastInsets.bottom,
                left = lastInsets.left,
                right = lastInsets.right
            )
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.i("UI started")
        // OctoPrint might not be available, this is more like a fire and forget
        // Don't bother user with error messages
        updateCapabilities("ui_start", escalateError = false)
    }

    override fun onStop() {
        super.onStop()
        Timber.i("UI stopped")
    }

    override fun onResume() {
        super.onResume()
        BillingManager.onResume(this)
        lifecycleScope.launchWhenResumed {
            BillingManager.billingEventFlow().collectLatest {
                it.consume { event ->
                    when (event) {
                        BillingEvent.PurchaseCompleted -> PurchaseConfirmationDialog().show(supportFragmentManager, "purchase-confirmation")
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        BillingManager.onPause()
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
                is WebSocketUpgradeFailedException -> e.exception?.let(this::showDialog)
                else -> setDisconnectedMessageVisible(!listOf(R.id.action_connect_printer, R.id.action_sign_in_required).contains(lastNavigation))
            }
        }

        Event.Connected -> {
            Timber.w("Connection restored")
            setDisconnectedMessageVisible(false)
        }

        is Event.MessageReceived -> onMessageReceived(e.message)
    }

    private fun onMessageReceived(e: SocketMessage) = when (e) {
        is SocketMessage.CurrentMessage -> onCurrentMessageReceived(e)
        is SocketMessage.EventMessage -> onEventMessageReceived(e)
        is SocketMessage.ConnectedMessage -> {
            // We are connected, let's update the available capabilities of the connect Octoprint
            if ((System.currentTimeMillis() - lastSuccessfulCapabilitiesUpdate) > 10000) {
                updateCapabilities("connected_event")
            } else Unit
        }
        else -> Unit
    }

    private fun onCurrentMessageReceived(e: SocketMessage.CurrentMessage) {
        Timber.tag("navigation").v(e.state?.flags.toString())
        val flags = e.state?.flags
        navigate(
            when {
                // We encountered an error, try reconnecting
                flags == null || flags.closedOrError || flags.error -> {
                    PrintNotificationService.stop(this)
                    R.id.action_connect_printer
                }

                // We are printing
                flags.printing || flags.paused || flags.pausing || flags.cancelling -> {
                    try {
                        PrintNotificationService.start(this)
                    } catch (e: IllegalStateException) {
                        // User might have closed app just in time so we can't start the service
                    }
                    R.id.action_printer_active
                }

                // We are connected
                flags.operational -> {
                    PrintNotificationService.stop(this)
                    R.id.action_printer_connected
                }

                !flags.operational && !flags.paused && !flags.cancelling && !flags.printing && !flags.pausing -> {
                    PrintNotificationService.stop(this)
                    R.id.action_connect_printer
                }

                // Fallback
                else -> lastNavigation
            }
        )
    }

    private fun onEventMessageReceived(e: SocketMessage.EventMessage) = when (e) {
        is SocketMessage.EventMessage.Connected, is SocketMessage.EventMessage.SettingsUpdated -> {
            // New printer connected or settings updated, let's update capabilities
            updateCapabilities("settings_updated")
        }
        else -> Unit
    }

    private fun setDisconnectedMessageVisible(visible: Boolean) {
        if (disconnectedMessage.isVisible == visible) return

        // Let disconnect message fill status bar background and measure height
        disconnectedMessage.updatePadding(
            top = disconnectedMessage.paddingBottom + lastInsets.top,
        )
        disconnectedMessage.measure(
            View.MeasureSpec.makeMeasureSpec(coordinatorLayout.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val height = disconnectedMessage.measuredHeight

        TransitionManager.beginDelayedTransition(coordinatorLayout, TransitionSet().apply {
            addTransition(Explode())
            addTransition(ChangeBounds())
            findCurrentScreen()?.view?.let {
                excludeChildren(it, true)
            }
        })
        disconnectedMessage.isVisible = visible
        findCurrentScreen()?.let { applyInsetsToScreen(it, height.takeIf { visible }) }
    }

    private fun updateCapabilities(trigger: String, escalateError: Boolean = true) {
        Timber.i("Updating capabities (trigger=$trigger)")
        lifecycleScope.launchWhenCreated {
            try {
                lastSuccessfulCapabilitiesUpdate = System.currentTimeMillis()
                Injector.get().updateInstanceCapabilitiesUseCase().execute(UpdateInstanceCapabilitiesUseCase.Params(updateM115 = escalateError))
                updateAllWidgets()
            } catch (e: Exception) {
                lastSuccessfulCapabilitiesUpdate = 0
                if (escalateError) {
                    Timber.e(e)
                    showDialog(getString(R.string.capabilities_validation_error))
                }
            }
        }
    }

    override fun startPrintNotificationService() {
        PrintNotificationService.start(this)
    }
}