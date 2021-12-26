package de.crysxd.octoapp

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.transition.ChangeBounds
import androidx.transition.Explode
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.analytics.FirebaseAnalytics
import de.crysxd.baseui.InsetAwareScreen
import de.crysxd.baseui.OctoActivity
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.common.OctoView
import de.crysxd.baseui.utils.ColorTheme
import de.crysxd.baseui.utils.colorTheme
import de.crysxd.baseui.widget.announcement.AnnouncementWidget
import de.crysxd.baseui.widget.extrude.ExtrudeWidget
import de.crysxd.baseui.widget.gcode.SendGcodeWidget
import de.crysxd.baseui.widget.quickaccess.PrePrintQuickAccessWidget
import de.crysxd.baseui.widget.quickaccess.PrintQuickAccessWidget
import de.crysxd.baseui.widget.temperature.ControlTemperatureWidget
import de.crysxd.baseui.widget.webcam.WebcamWidget
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.billing.BillingEvent
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.billing.BillingManager.FEATURE_QUICK_SWITCH
import de.crysxd.octoapp.base.data.models.WidgetType
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.usecase.GetRemoteServiceConnectUrlUseCase.Companion.OCTOEVERYWHERE_APP_PORTAL_CALLBACK_PATH
import de.crysxd.octoapp.base.usecase.GetRemoteServiceConnectUrlUseCase.Companion.SPAGHETTI_DETECTIVE_APP_PORTAL_CALLBACK_PATH
import de.crysxd.octoapp.base.usecase.UpdateInstanceCapabilitiesUseCase
import de.crysxd.octoapp.databinding.MainActivityBinding
import de.crysxd.octoapp.notification.LiveNotificationManager
import de.crysxd.octoapp.octoprint.exceptions.WebSocketMaybeBrokenException
import de.crysxd.octoapp.octoprint.exceptions.WebSocketUpgradeFailedException
import de.crysxd.octoapp.octoprint.models.ConnectionType
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.preprintcontrols.ui.widget.move.MoveToolWidget
import de.crysxd.octoapp.printcontrols.ui.widget.gcode.GcodePreviewWidget
import de.crysxd.octoapp.printcontrols.ui.widget.progress.ProgressWidget
import de.crysxd.octoapp.printcontrols.ui.widget.tune.TuneWidget
import de.crysxd.octoapp.signin.di.SignInInjector
import de.crysxd.octoapp.widgets.updateAllWidgets
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import timber.log.Timber
import java.util.Date
import de.crysxd.octoapp.octoprint.models.socket.Message as SocketMessage
import de.crysxd.octoapp.preprintcontrols.di.PrePrintControlsInjector as ConnectPrinterInjector

class MainActivity : OctoActivity() {

    companion object {
        const val EXTRA_TARGET_OCTOPRINT_ID = "octoprint_id"
        const val EXTRA_CLICK_URI = "clickUri"
    }

    private lateinit var binding: MainActivityBinding
    private val viewModel by lazy { ViewModelProvider(this)[MainActivityViewModel::class.java] }
    private val lastInsets = Rect()
    override val octoToolbar: OctoToolbar by lazy { binding.toolbar }
    override val octo: OctoView by lazy { binding.toolbarOctoView }
    override val rootLayout by lazy { binding.coordinator }
    override val navController get() = findNavController(R.id.mainNavController)
    private var enforceAutoamticNavigationAllowed = false
    private var uiStoppedAt: Long? = null
    private var updateCapabilitiesJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // We need to call this before nav component grabs any link. onNewIntent
        // handles any links and removes them from the intent
        onNewIntent(intent)

        binding = MainActivityBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        // Fix fullscreen layout under system bars for frame layout
        rootLayout.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)

        // Observe events, will update when instance changes
        ConnectPrinterInjector.get().octoprintProvider().eventFlow("MainActivity@events")
            .asLiveData()
            .map { it }
            .observe(this, ::onEventReceived)

        ConnectPrinterInjector.get().octoprintProvider().passiveCurrentMessageFlow("MainActivity@currentMessage")
            .asLiveData()
            .map { it }
            .observe(this, ::onCurrentMessageReceived)

        ConnectPrinterInjector.get().octoprintProvider().passiveConnectionEventFlow("MainActivity@connectionType")
            .asLiveData()
            .map { it }
            .observe(this) {
                viewModel.connectionType = it.connectionType
                updateConnectionBanner(false)
            }

        // Inflate widgets
        octoWidgetRecycler.setWidgetFactory(this) {
            when (it) {
                WidgetType.AnnouncementWidget -> AnnouncementWidget(this@MainActivity)
                WidgetType.ControlTemperatureWidget -> ControlTemperatureWidget(this@MainActivity)
                WidgetType.ExtrudeWidget -> ExtrudeWidget(this@MainActivity)
                WidgetType.GcodePreviewWidget -> GcodePreviewWidget(this@MainActivity)
                WidgetType.MoveToolWidget -> MoveToolWidget(this@MainActivity)
                WidgetType.PrePrintQuickAccessWidget -> PrePrintQuickAccessWidget(this@MainActivity)
                WidgetType.PrintQuickAccessWidget -> PrintQuickAccessWidget(this@MainActivity)
                WidgetType.ProgressWidget -> ProgressWidget(this@MainActivity)
                WidgetType.QuickAccessWidget -> throw IllegalStateException("Can't create abstract class")
                WidgetType.SendGcodeWidget -> SendGcodeWidget(this@MainActivity)
                WidgetType.TuneWidget -> TuneWidget(this@MainActivity)
                WidgetType.WebcamWidget -> WebcamWidget(this@MainActivity)
            }
        }

        SignInInjector.get().octoprintRepository().instanceInformationFlow()
            .filter {
                val webUrlAndApiKey = "${it?.webUrl}:${it?.apiKey}:${it?.issue}"
                val pass = viewModel.lastWebUrlAndApiKey != webUrlAndApiKey
                viewModel.lastWebUrlAndApiKey = webUrlAndApiKey
                Timber.i("Instance information filter $it => $pass")
                pass
            }
            .asLiveData()
            .observe(this) { instance ->
                when {
                    instance != null && (instance.apiKey.isBlank() || instance.issue != null) -> {
                        Timber.i("Instance information received without API key: $instance")
                        showDialog(
                            message = getString(instance.issue?.messageRes ?: R.string.sign_in___broken_setup___api_key_revoked, instance.issueMessage ?: ""),
                            positiveAction = {
                                if (instance.issue?.isForAlternative != true) {
                                    UriLibrary.getFixOctoPrintConnectionUri(baseUrl = instance.webUrl, instanceId = instance.id).open(this)
                                } else {
                                    UriLibrary.getConfigureRemoteAccessUri().open(this)
                                }
                            },
                            positiveButton = getString(R.string.sign_in___continue),
                            highPriority = true
                        )
                    }

                    instance != null && instance.apiKey.isNotBlank() -> {
                        Timber.i("Instance information received $this")
                        updateCapabilities("instance_change", updateM115 = true, escalateError = false)
                        navigate(R.id.action_connect_printer)
                        viewModel.pendingUri?.let {
                            viewModel.pendingUri = null
                            handleDeepLink(it)
                        }
                    }

                    else -> {
                        Timber.i("No instance active $this")
                        navigate(R.id.action_sign_in_required)
                        LiveNotificationManager.stop(this)
                    }
                }
            }

        SignInInjector.get().octoprintRepository().instanceInformationFlow()
            .distinctUntilChangedBy { it?.settings?.appearance?.color }
            .asLiveData()
            .observe(this) {
                ColorTheme.applyColorTheme(it.colorTheme)
            }

        lifecycleScope.launchWhenResumed {
            val navHost = supportFragmentManager.findFragmentById(R.id.mainNavController) as NavHostFragment

            navController.addOnDestinationChangedListener { _, destination, _ ->
                Timber.i("Navigated to ${destination.label}")
                OctoAnalytics.logEvent(OctoAnalytics.Event.ScreenShown, bundleOf(FirebaseAnalytics.Param.SCREEN_NAME to destination.label?.toString()))

                when (destination.id) {
                    R.id.discoverFragment -> OctoAnalytics.logEvent(OctoAnalytics.Event.LoginWorkspaceShown)
                    R.id.workspaceConnect -> OctoAnalytics.logEvent(OctoAnalytics.Event.ConnectWorkspaceShown)
                    R.id.workspacePrePrint -> OctoAnalytics.logEvent(OctoAnalytics.Event.PrePrintWorkspaceShown)
                    R.id.workspacePrint -> OctoAnalytics.logEvent(OctoAnalytics.Event.PrintWorkspaceShown)
                    R.id.terminalFragment -> OctoAnalytics.logEvent(OctoAnalytics.Event.TerminalWorkspaceShown)
                }

                viewModel.pendingNavigation?.let {
                    viewModel.pendingNavigation = null
                    navigate(it)
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
                lastInsets.top = insets.systemWindowInsetTop
                lastInsets.left = insets.systemWindowInsetLeft
                lastInsets.bottom = insets.systemWindowInsetBottom
                lastInsets.right = insets.systemWindowInsetRight
                applyInsetsToCurrentScreen()
                setBannerVisible(binding.bannerView.isVisible)
                insets.consumeSystemWindowInsets()
            }
        }

        if (!isTablet() && !BaseInjector.get().octoPreferences().allowAppRotation) {
            // Stop screen rotation on phones
            @SuppressLint("SourceLockedOrientationActivity")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    override fun onNewIntent(intent: Intent?) {
        if (BillingManager.isFeatureEnabled(FEATURE_QUICK_SWITCH)) {
            intent?.getStringExtra(EXTRA_TARGET_OCTOPRINT_ID)?.let { id ->
                val repo = BaseInjector.get().octorPrintRepository()
                repo.get(id)?.let {
                    repo.setActive(it)
                }
            }
        }

        intent?.data?.let {
            Timber.i("Handling URI: $it")
            if (it.host == "app.octoapp.eu" || it.host == "test.octoapp.eu") {
                // Give a second for everything to settle
                handleDeepLink(it)
            } else {
                Timber.w("Dropping URI, host is ${it.host}")
            }
        }

        intent?.getStringExtra(EXTRA_CLICK_URI)?.let {
            try {
                val uri = Uri.parse(it)
                Timber.i("Handling click URI: $uri")
                if (uri.host == "app.octoapp.eu") {
                    // Give a second for everything to settle
                    handleDeepLink(uri)
                } else {
                    Timber.w("Dropping URI, host is ${uri.host}")
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        // Clean data so the nav component doesn't grab the link. We need to do this manually honoring the app state
        intent?.data = null
        super.onNewIntent(intent)
    }

    private fun handleDeepLink(uri: Uri) {
        Timber.i("Hanlding deep link2")

        lifecycleScope.launchWhenResumed {
            try {
                if (UriLibrary.isActiveInstanceRequired(uri) && BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot() == null) {
                    Timber.i("Uri requires active instance, delaying")
                    viewModel.pendingUri = uri
                } else {
                    Timber.i("Hanlding deep link")
                    when (uri.path) {
                        "/$OCTOEVERYWHERE_APP_PORTAL_CALLBACK_PATH" -> BaseInjector.get().handleOctoEverywhereAppPortalSuccessUseCase().execute(uri)
                        "/$SPAGHETTI_DETECTIVE_APP_PORTAL_CALLBACK_PATH" -> BaseInjector.get().handleSpaghettiDetectiveAppPortalSuccessUseCase().execute(uri)
                        else -> {
                            Timber.i("Handling generic URI: $uri")
                            uri.open(this@MainActivity)
                        }
                    }
                }
            } catch (e: Exception) {
                showDialog(e)
            }
        }
    }

    private fun isTablet() = ((this.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE)

    private fun applyInsetsToCurrentScreen() = findCurrentScreen()?.let { applyInsetsToScreen(it) }

    private fun findCurrentScreen() = supportFragmentManager.findFragmentById(R.id.mainNavController)?.childFragmentManager?.fragments?.firstOrNull()

    private fun applyInsetsToScreen(screen: Fragment, topOverwrite: Int? = null) {
        val bannerHeight = binding.bannerView.height.takeIf { binding.bannerView.isVisible }
        Timber.v("Applying insets: bannerView=$bannerHeight topOverwrite=$topOverwrite screen=$screen")
        binding.toolbar.updateLayoutParams<FrameLayout.LayoutParams> { topMargin = topOverwrite ?: bannerHeight ?: lastInsets.top }
        octo.updateLayoutParams<FrameLayout.LayoutParams> { topMargin = topOverwrite ?: bannerHeight ?: lastInsets.top }

        if (screen is InsetAwareScreen) {
            screen.handleInsets(
                Rect(
                    lastInsets.left,
                    topOverwrite ?: bannerHeight ?: lastInsets.top,
                    lastInsets.right,
                    lastInsets.bottom,
                )
            )
        } else {
            screen.view?.updatePadding(
                top = topOverwrite ?: bannerHeight ?: lastInsets.top,
                bottom = lastInsets.bottom,
                left = lastInsets.left,
                right = lastInsets.right
            )
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.i("UI started")

        val stoppedAt = uiStoppedAt
        if (stoppedAt != null && (System.currentTimeMillis() - stoppedAt) > 30_000) {
            // OctoPrint might not be available, this is more like a fire and forget
            // Don't bother user with error messages
            updateCapabilities("ui_start", updateM115 = false, escalateError = false)
        } else {
            Timber.i("Ui stopped for less than 30s, skipping capabilities update")
        }

        updateConnectionBanner(alreadyShrunken = true)
    }

    override fun onStop() {
        super.onStop()
        val now = System.currentTimeMillis()
        uiStoppedAt = now
        Timber.i("UI stopped at ${Date(now)}")
    }

    override fun onResume() {
        super.onResume()
        BaseInjector.get().octoPreferences().wasPrintNotificationDisabledUntilNextLaunch = false
        BillingManager.onResume(this)
        lifecycleScope.launchWhenResumed {
            BillingManager.billingEventFlow().collectLatest {
                it.consume { event ->
                    when (event) {
                        BillingEvent.PurchaseCompleted -> de.crysxd.baseui.purchase.PurchaseConfirmationDialog()
                            .show(supportFragmentManager, "purchase-confirmation")
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        BillingManager.onPause()
    }

    override fun enforceAllowAutomaticNavigationFromCurrentDestination() {
        Timber.i("Enforcing auto navigation for the next navigation event")
        enforceAutoamticNavigationAllowed = true
    }

    private fun navigate(id: Int) {
        if (id != viewModel.lastNavigation) {
            // Screens which must be/can be closed automatically when the state changes
            // Other screens will stay open and we navigate to the new state-based destination after the
            // current screen is closed
            val currentDestination = navController.currentDestination?.id
            val currentDestinationAllowsAutoNavigate = listOf(
                R.id.splashFragment,
                R.id.discoverFragment,
                R.id.requestAccessFragment,
                R.id.signInSuccessFragment,
                R.id.workspaceConnect,
                R.id.workspacePrePrint,
                R.id.workspacePrint,
                R.id.terminalFragment,
            ).contains(currentDestination)
            val destinationName = currentDestination?.let(resources::getResourceEntryName)

            if (currentDestinationAllowsAutoNavigate || enforceAutoamticNavigationAllowed) {
                Timber.v("Navigating to $destinationName (currentDestinationAllowsAutoNavigate=$currentDestinationAllowsAutoNavigate enforceAutoamticNavigationAllowed=$enforceAutoamticNavigationAllowed)")
                enforceAutoamticNavigationAllowed = false
                viewModel.lastNavigation = id
                navController.navigate(id)
            } else {
                Timber.v("Current destination $destinationName does not allow auto navigate, storing navigation action as pending")
                viewModel.pendingNavigation = id
            }
        }
    }

    private fun onEventReceived(e: Event) = when (e) {
        // Only show errors if we are not already in disconnected screen. We still want to show the stall warning to indicate something is wrong
        // as this might lead to the user being stuck
        is Event.Disconnected -> {
            Timber.w("Connection lost")
            when {
                e.exception is WebSocketMaybeBrokenException -> e.exception?.let(this::showDialog)
                e.exception is WebSocketUpgradeFailedException -> e.exception?.let(this::showDialog)
                !listOf(R.id.action_connect_printer, R.id.action_sign_in_required).contains(viewModel.lastNavigation) ->
                    showBanner(R.string.main___banner_connection_lost_reconnecting, 0, R.color.yellow, showSpinner = true, alreadyShrunken = false)
                else -> Unit
            }
        }

        is Event.Connected -> {
            Timber.w("Connection restored")
            updateAllWidgets()

            // Start LiveNotification again, might have been stopped!
            LiveNotificationManager.restartIfWasPaused(this)
        }

        is Event.MessageReceived -> {
            if (viewModel.connectionType == ConnectionType.Primary) {
                setBannerVisible(false)
            }

            onMessageReceived(e.message)
        }

        else -> Unit
    }

    private fun updateConnectionBanner(alreadyShrunken: Boolean) {
        when (viewModel.connectionType) {
            null -> setBannerVisible(false)

            ConnectionType.Primary -> if (viewModel.previousConnectionType != null && viewModel.previousConnectionType != ConnectionType.Primary) {
                // If we switched back from a alternative connection to primary, show banner
                showBanner(
                    R.string.main___banner_connected_via_local,
                    null,
                    R.color.green,
                    showSpinner = false,
                    alreadyShrunken = false,
                    doOnShrink = { setBannerVisible(false) }
                )
            } else {
                Timber.d("Previous connection type was ${viewModel.previousConnectionType}, not showing local banner")
                setBannerVisible(false)
            }

            ConnectionType.Alternative -> showBanner(
                R.string.main___banner_connected_via_alternative,
                R.drawable.ic_round_cloud_queue_24,
                R.color.blue,
                showSpinner = false,
                alreadyShrunken = alreadyShrunken
            )

            ConnectionType.Tailscale -> showBanner(
                R.string.main___banner_connected_via_tailscale,
                R.drawable.ic_tailscale_24px,
                R.color.tailscale,
                showSpinner = false,
                alreadyShrunken = alreadyShrunken
            )

            ConnectionType.Ngrok -> showBanner(
                R.string.main___banner_connected_via_ngrok,
                R.drawable.ic_ngrok_24px,
                R.color.ngrok,
                showSpinner = false,
                alreadyShrunken = alreadyShrunken
            )

            ConnectionType.OctoEverywhere -> showBanner(
                R.string.main___banner_connected_via_octoeverywhere,
                R.drawable.ic_octoeverywhere_24px,
                R.color.octoeverywhere,
                showSpinner = false,
                alreadyShrunken = alreadyShrunken
            )

            ConnectionType.SpaghettiDetective -> showBanner(
                R.string.main___banner_connected_via_spaghetti_detective,
                R.drawable.ic_spaghetti_detective_24px,
                R.color.spaghetti_detective,
                showSpinner = false,
                alreadyShrunken = alreadyShrunken
            )
        }.hashCode()
    }

    private fun onMessageReceived(e: SocketMessage) = when (e) {
        is SocketMessage.CurrentMessage -> onCurrentMessageReceived(e)
        is SocketMessage.EventMessage -> onEventMessageReceived(e)
        is SocketMessage.ConnectedMessage -> {
            // We are connected, let's update the available capabilities of the connect Octoprint
            if ((System.currentTimeMillis() - viewModel.lastSuccessfulCapabilitiesUpdate) > 10000) {
                updateCapabilities("connected_event")
            } else Unit
        }
        else -> Unit
    }

    private fun onCurrentMessageReceived(e: SocketMessage.CurrentMessage) {
        Timber.tag("navigation").v(e.state?.flags.toString())
        val flags = e.state?.flags
        val lastFlags = viewModel.lastFlags
        viewModel.lastFlags = flags
        if (flags == lastFlags) {
            viewModel.sameFlagsCounter++
        } else {
            viewModel.sameFlagsCounter = 0
        }

        // Sometimes when changing e.g. from paused to printing OctoPrint sends one wrong set of flags, so we
        // only continue if last and current are the same
        // If we have closed or error, it's always instant
        if ((viewModel.sameFlagsCounter < 3 || lastFlags == null) && flags?.closedOrError != true) {
            return Timber.i("Skipping flag navigation, recently changed and waiting for confirmation")
        }

        if ((viewModel.sameFlagsCounter == 3 && lastFlags != null) && flags?.closedOrError != true) {
            Timber.i("Performing flag navigation: $flags")
        }

        navigate(
            when {
                // We encountered an error, try reconnecting
                flags == null || flags.isError() -> {
                    LiveNotificationManager.stop(this)
                    R.id.action_connect_printer
                }

                // We are printing
                flags.isPrinting() -> {
                    try {
                        LiveNotificationManager.start(this)
                    } catch (e: IllegalStateException) {
                        // User might have closed app just in time so we can't start the service
                    }
                    R.id.action_printer_active
                }

                // We are connected
                flags.isOperational() -> {
                    LiveNotificationManager.stop(this)
                    R.id.action_printer_connected
                }

                !flags.isOperational() && !flags.isPrinting() -> {
                    LiveNotificationManager.stop(this)
                    R.id.action_connect_printer
                }

                // Fallback
                else -> viewModel.lastNavigation
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

    private fun showBanner(
        @StringRes text: Int,
        @DrawableRes icon: Int?,
        @ColorRes background: Int,
        showSpinner: Boolean,
        alreadyShrunken: Boolean,
        doOnShrink: () -> Unit = {}
    ) {
        binding.bannerView.show(this, text, icon, background, showSpinner, alreadyShrunken, doOnShrink)
        setBannerVisible(true)
    }

    private fun setBannerVisible(visible: Boolean) {
        // Not visible and we should not be visible? Nothing to do.
        // If we are visible or should be visible, we need to update height as insets might have changed
        if (!binding.bannerView.isVisible && !visible) {
            return
        }

        // Not layed out yet? Do later
        if (rootLayout.width == 0) {
            rootLayout.doOnNextLayout { setBannerVisible(visible) }
            return
        }

        // Let disconnect message fill status bar background and measure height
        binding.bannerView.updatePadding(top = lastInsets.top)
        binding.bannerView.measure(
            View.MeasureSpec.makeMeasureSpec(rootLayout.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val height = binding.bannerView.measuredHeight

        fun runTransition() = TransitionManager.beginDelayedTransition(rootLayout, TransitionSet().apply {
            addTransition(Explode())
            addTransition(ChangeBounds())
            addTransition(Fade())
            excludeTarget(de.crysxd.baseui.R.id.widgetContainer, true)
            excludeTarget(ViewPager2::class.java, true)
            excludeChildren(octoToolbar, true)
        })

        // Shrinking after delay
        runTransition()
        binding.bannerView.onStartShrink = {
            runTransition()
            binding.bannerView.doOnNextLayout {
                setBannerVisible(true)
            }
        }

        if (!visible) {
            binding.bannerView.hide()
        }

        binding.bannerView.isVisible = visible
        findCurrentScreen()?.let { applyInsetsToScreen(it, height.takeIf { visible }) }
    }

    private fun updateCapabilities(trigger: String, updateM115: Boolean = true, escalateError: Boolean = true) {
        Timber.i("Updating capabities (trigger=$trigger)")
        updateCapabilitiesJob = lifecycleScope.launchWhenCreated {
            try {
                viewModel.lastSuccessfulCapabilitiesUpdate = System.currentTimeMillis()
                BaseInjector.get().updateInstanceCapabilitiesUseCase().execute(UpdateInstanceCapabilitiesUseCase.Params(updateM115 = updateM115))
                updateAllWidgets()
            } catch (e: Exception) {
                viewModel.lastSuccessfulCapabilitiesUpdate = 0
                if (escalateError) {
                    Timber.e(e)
                    showSnackbar(
                        Message.SnackbarMessage(
                            text = { getString(R.string.capabilities_validation_error) },
                            type = Message.SnackbarMessage.Type.Negative
                        )
                    )
                }
            }
        }
    }

    override fun startPrintNotificationService() {
        LiveNotificationManager.start(this)
    }
}