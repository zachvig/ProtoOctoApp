package de.crysxd.octoapp

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
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
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ui.ColorTheme
import de.crysxd.octoapp.base.ui.base.InsetAwareScreen
import de.crysxd.octoapp.base.ui.base.OctoActivity
import de.crysxd.octoapp.base.ui.colorTheme
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.common.OctoView
import de.crysxd.octoapp.base.ui.widget.announcement.AnnouncementWidget
import de.crysxd.octoapp.base.ui.widget.gcode.SendGcodeWidget
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidget
import de.crysxd.octoapp.base.usecase.UpdateInstanceCapabilitiesUseCase
import de.crysxd.octoapp.databinding.MainActivityBinding
import de.crysxd.octoapp.octoprint.exceptions.WebSocketMaybeBrokenException
import de.crysxd.octoapp.octoprint.exceptions.WebSocketUpgradeFailedException
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.pre_print_controls.ui.widget.extrude.ExtrudeWidget
import de.crysxd.octoapp.pre_print_controls.ui.widget.move.MoveToolWidget
import de.crysxd.octoapp.print_controls.ui.widget.gcode.GcodePreviewWidget
import de.crysxd.octoapp.print_controls.ui.widget.progress.ProgressWidget
import de.crysxd.octoapp.print_controls.ui.widget.tune.TuneWidget
import de.crysxd.octoapp.widgets.updateAllWidgets
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import timber.log.Timber
import de.crysxd.octoapp.octoprint.models.socket.Message as SocketMessage
import de.crysxd.octoapp.pre_print_controls.di.Injector as ConnectPrinterInjector
import de.crysxd.octoapp.signin.di.Injector as SignInInjector

const val KEY_LAST_NAVIGATION = "lastNavigation"
const val KEY_LAST_WEB_URL = "lastWebUrl"
const val EXTRA_TARGET_OCTOPRINT_WEB_URL = "octoprint_web_url"

class MainActivity : OctoActivity() {

    private lateinit var binding: MainActivityBinding
    private var lastNavigation = -1
    private var lastWebUrl: String? = "initial"
    private val lastInsets = Rect()
    private var lastSuccessfulCapabilitiesUpdate = 0L

    override val octoToolbar: OctoToolbar by lazy { binding.toolbar }
    override val octo: OctoView by lazy { binding.toolbarOctoView }
    override val rootLayout by lazy { binding.coordinator }
    override val navController get() = findNavController(R.id.mainNavController)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        // Fix fullscreen layout under system bars for frame layout
        rootLayout.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

        val eventObserver = Observer(this::onEventReceived)
        val currentMessageObserver = Observer(this::onCurrentMessageReceived)
        val events = ConnectPrinterInjector.get().octoprintProvider().eventFlow("MainActivity@events").asLiveData().map {
            it
        }
        val currentMessages = ConnectPrinterInjector.get().octoprintProvider().passiveCurrentMessageFlow("MainActivity@currentMessage").asLiveData().map {
            it
        }

        // Inflate widgets
        octoWidgetRecycler.preInflateWidget(this) { AnnouncementWidget(this@MainActivity) }
        octoWidgetRecycler.preInflateWidget(this) { MoveToolWidget(this@MainActivity) }
        octoWidgetRecycler.preInflateWidget(this) { ExtrudeWidget(this@MainActivity) }
        octoWidgetRecycler.preInflateWidget(this) { ControlTemperatureWidget(this@MainActivity) }
        octoWidgetRecycler.preInflateWidget(this) { SendGcodeWidget(this@MainActivity) }
        octoWidgetRecycler.preInflateWidget(this) { WebcamWidget(this@MainActivity) }
        octoWidgetRecycler.preInflateWidget(this) { GcodePreviewWidget(this@MainActivity) }
        octoWidgetRecycler.preInflateWidget(this) { ProgressWidget(this@MainActivity) }
        octoWidgetRecycler.preInflateWidget(this) { TuneWidget(this@MainActivity) }

        onNewIntent(intent)
        lastWebUrl = savedInstanceState?.getString(KEY_LAST_WEB_URL) ?: lastWebUrl
        lastNavigation = savedInstanceState?.getInt(KEY_LAST_NAVIGATION, lastNavigation) ?: lastNavigation
        Timber.i("onCreate $lastWebUrl")

        SignInInjector.get().octoprintRepository().instanceInformationFlow()
            .filter {
                val pass = lastWebUrl != it?.webUrl
                lastWebUrl = it?.webUrl
                pass
            }
            .asLiveData()
            .observe(this, {
                Timber.i("Instance information received $this")
                updateAllWidgets()
                if (it != null && it.apiKey.isNotBlank()) {
                    updateCapabilities("instance_change", updateM115 = true, escalateError = false)
                    navigate(R.id.action_connect_printer)
                    events.observe(this, eventObserver)
                    currentMessages.observe(this, currentMessageObserver)
                } else {
                    navigate(R.id.action_sign_in_required)
                    PrintNotificationService.stop(this)
                    (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(PrintNotificationService.NOTIFICATION_ID)
                    events.removeObserver(eventObserver)
                    currentMessages.removeObserver(currentMessageObserver)
                }
            })

        SignInInjector.get().octoprintRepository().instanceInformationFlow()
            .distinctUntilChangedBy { it?.settings?.appearance?.color }
            .asLiveData()
            .observe(this) { ColorTheme.applyColorTheme(it.colorTheme) }

        lifecycleScope.launchWhenResumed {
            val navHost = supportFragmentManager.findFragmentById(R.id.mainNavController) as NavHostFragment

            navController.addOnDestinationChangedListener { _, destination, _ ->
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

            navHost.childFragmentManager.registerFragmentLifecycleCallbacks(
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
                Timber.d("Insets updated $insets")
                lastInsets.top = insets.systemWindowInsetTop
                lastInsets.left = insets.systemWindowInsetLeft
                lastInsets.bottom = insets.systemWindowInsetBottom
                lastInsets.right = insets.systemWindowInsetRight
                applyInsetsToCurrentScreen()
                setDisconnectedMessageVisible(binding.disconnectedMessage.isVisible)
                insets.consumeSystemWindowInsets()
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
        if (BillingManager.isFeatureEnabled("quick_switch")) {
            intent?.getStringExtra(EXTRA_TARGET_OCTOPRINT_WEB_URL)?.let { webUrl ->
                val repo = Injector.get().octorPrintRepository()
                repo.getAll().firstOrNull { it.webUrl == webUrl }?.let {
                    repo.setActive(it)
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            intent?.data?.let {
                if (it.host == "app.octoapp.eu") {
                    // Give a second for everything to settle
                    delay(1000)
                    it.open(this@MainActivity)
                }
            }
        }
    }

    private fun isTablet() = ((this.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_LAST_NAVIGATION, lastNavigation)
        outState.putString(KEY_LAST_WEB_URL, lastWebUrl)
    }

    private fun applyInsetsToCurrentScreen() = findCurrentScreen()?.let { applyInsetsToScreen(it) }

    private fun findCurrentScreen() = supportFragmentManager.findFragmentById(R.id.mainNavController)?.childFragmentManager?.fragments?.firstOrNull()

    private fun applyInsetsToScreen(screen: Fragment, topOverwrite: Int? = null) {
        val disconnectHeight = binding.disconnectedMessage.height.takeIf { binding.disconnectedMessage.isVisible }
        Timber.v("Applying insets: disconnectedMessage=$disconnectHeight topOverwrite=$topOverwrite screen=$screen")
        binding.toolbar.updateLayoutParams<FrameLayout.LayoutParams> { topMargin = topOverwrite ?: disconnectHeight ?: lastInsets.top }
        octo.updateLayoutParams<FrameLayout.LayoutParams> { topMargin = topOverwrite ?: disconnectHeight ?: lastInsets.top }

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
        updateCapabilities("ui_start", updateM115 = false, escalateError = false)
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
            navController.navigate(id)
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
                flags == null || flags.isError() -> {
                    PrintNotificationService.stop(this)
                    R.id.action_connect_printer
                }

                // We are printing
                flags.isPrinting() -> {
                    try {
                        PrintNotificationService.start(this)
                    } catch (e: IllegalStateException) {
                        // User might have closed app just in time so we can't start the service
                    }
                    R.id.action_printer_active
                }

                // We are connected
                flags.isOperational() -> {
                    PrintNotificationService.stop(this)
                    R.id.action_printer_connected
                }

                !flags.isOperational() && !flags.isPrinting() -> {
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
            updateCapabilities("settings_updated", updateM115 = false)
        }
        else -> Unit
    }

    private fun setDisconnectedMessageVisible(visible: Boolean) {
        // Not visible and we should not be visible? Nothing to do.
        // If we are visible or should be visible, we need to update height as insets might have changed
        if (!binding.disconnectedMessage.isVisible && !visible) {
            return
        }

        // Let disconnect message fill status bar background and measure height
        binding.disconnectedMessage.updatePadding(
            top = binding.disconnectedMessage.paddingBottom + lastInsets.top,
        )
        binding.disconnectedMessage.measure(
            View.MeasureSpec.makeMeasureSpec(rootLayout.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val height = binding.disconnectedMessage.measuredHeight

        TransitionManager.beginDelayedTransition(rootLayout, TransitionSet().apply {
            addTransition(Explode())
            addTransition(ChangeBounds())
            excludeChildren(octoToolbar, true)
        })
        binding.disconnectedMessage.isVisible = visible
        findCurrentScreen()?.let { applyInsetsToScreen(it, height.takeIf { visible }) }
    }

    private fun updateCapabilities(trigger: String, updateM115: Boolean = true, escalateError: Boolean = true) {
        Timber.i("Updating capabities (trigger=$trigger)")
        lifecycleScope.launchWhenCreated {
            try {
                lastSuccessfulCapabilitiesUpdate = System.currentTimeMillis()
                Injector.get().updateInstanceCapabilitiesUseCase().execute(UpdateInstanceCapabilitiesUseCase.Params(updateM115 = updateM115))
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