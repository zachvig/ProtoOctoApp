package de.crysxd.octoapp

import android.annotation.SuppressLint
import android.app.NotificationManager
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
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.firebase.analytics.FirebaseAnalytics
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.billing.BillingEvent
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.billing.BillingManager.FEATURE_QUICK_SWITCH
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
import de.crysxd.octoapp.base.ui.widget.extrude.ExtrudeWidget
import de.crysxd.octoapp.base.ui.widget.gcode.SendGcodeWidget
import de.crysxd.octoapp.base.ui.widget.temperature.ControlTemperatureWidget
import de.crysxd.octoapp.base.ui.widget.webcam.WebcamWidget
import de.crysxd.octoapp.base.usecase.OCTOEVERYWHERE_APP_PORTAL_CALLBACK_PATH
import de.crysxd.octoapp.base.usecase.UpdateInstanceCapabilitiesUseCase
import de.crysxd.octoapp.databinding.MainActivityBinding
import de.crysxd.octoapp.notification.NOTIFICATION_ID
import de.crysxd.octoapp.notification.PrintNotificationManager
import de.crysxd.octoapp.octoprint.exceptions.WebSocketMaybeBrokenException
import de.crysxd.octoapp.octoprint.exceptions.WebSocketUpgradeFailedException
import de.crysxd.octoapp.octoprint.models.ConnectionType
import de.crysxd.octoapp.octoprint.models.socket.Event
import de.crysxd.octoapp.pre_print_controls.ui.widget.move.MoveToolWidget
import de.crysxd.octoapp.print_controls.ui.widget.gcode.GcodePreviewWidget
import de.crysxd.octoapp.print_controls.ui.widget.progress.ProgressWidget
import de.crysxd.octoapp.print_controls.ui.widget.tune.TuneWidget
import de.crysxd.octoapp.widgets.updateAllWidgets
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import timber.log.Timber
import de.crysxd.octoapp.octoprint.models.socket.Message as SocketMessage
import de.crysxd.octoapp.pre_print_controls.di.Injector as ConnectPrinterInjector
import de.crysxd.octoapp.signin.di.Injector as SignInInjector

const val EXTRA_TARGET_OCTOPRINT_WEB_URL = "octoprint_web_url"

class MainActivity : OctoActivity() {

    private lateinit var binding: MainActivityBinding
    private val viewModel by lazy { ViewModelProvider(this)[MainActivityViewModel::class.java] }
    private val lastInsets = Rect()
    override val octoToolbar: OctoToolbar by lazy { binding.toolbar }
    override val octo: OctoView by lazy { binding.toolbarOctoView }
    override val rootLayout by lazy { binding.coordinator }
    override val navController get() = findNavController(R.id.mainNavController)

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

        SignInInjector.get().octoprintRepository().instanceInformationFlow()
            .filter {
                val webUrlAndApiKey = "${it?.webUrl}:${it?.apiKey}"
                val pass = viewModel.lastWebUrlAndApiKey != webUrlAndApiKey
                viewModel.lastWebUrlAndApiKey = webUrlAndApiKey
                pass
            }
            .asLiveData()
            .observe(this) {
                if (it != null && it.apiKey.isNotBlank()) {
                    Timber.i("Instance information received $this")
                    updateCapabilities("instance_change", updateM115 = true, escalateError = false)
                    navigate(R.id.action_connect_printer)
                    viewModel.pendingUri?.let {
                        viewModel.pendingUri = null
                        handleDeepLink(it)
                    }
                } else {
                    Timber.i("No instance active $this")
                    navigate(R.id.action_sign_in_required)
                    PrintNotificationManager.stop(this)
                    (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID)
                }
            }

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

        if (!isTablet() && !Injector.get().octoPreferences().allowAppRotation) {
            // Stop screen rotation on phones
            @SuppressLint("SourceLockedOrientationActivity")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    override fun onNewIntent(intent: Intent?) {
        if (BillingManager.isFeatureEnabled(FEATURE_QUICK_SWITCH)) {
            intent?.getStringExtra(EXTRA_TARGET_OCTOPRINT_WEB_URL)?.let { webUrl ->
                val repo = Injector.get().octorPrintRepository()
                repo.getAll().firstOrNull { it.webUrl == webUrl }?.let {
                    repo.setActive(it)
                }
            }
        }

        intent?.data?.let {
            Timber.i("Handling URI: $it")
            if (it.host == "app.octoapp.eu") {
                // Give a second for everything to settle
                handleDeepLink(it)
            }
        }

        // Clean data so the nav component doesn't grab the link. We need to do this manually honoring the app state
        intent?.data = null
        super.onNewIntent(intent)
    }

    private fun handleDeepLink(uri: Uri) {
        if (UriLibrary.isActiveInstanceRequired(uri) && Injector.get().octorPrintRepository().getActiveInstanceSnapshot() == null) {
            Timber.i("Uri requires active instance, delaying")
            viewModel.pendingUri = uri
        } else {
            if (uri.path == "/$OCTOEVERYWHERE_APP_PORTAL_CALLBACK_PATH") {
                // Uh yeah, new OctoEverywhere connection
                lifecycleScope.launchWhenCreated {
                    try {
                        Timber.i("Handling OctoEverywhere connection")
                        Injector.get().handleOctoEverywhereAppPortalSuccessUseCase().execute(uri)
                    } catch (e: Exception) {
                        showDialog(e)
                    }
                }
            } else {
                // Generic link
                Timber.i("Handling generic URI: $uri")
                uri.open(this@MainActivity)
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
        // OctoPrint might not be available, this is more like a fire and forget
        // Don't bother user with error messages
        updateCapabilities("ui_start", updateM115 = false, escalateError = false)
        updateConnectionBanner(alreadyShrunken = true)
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
        if (id != viewModel.lastNavigation) {
            // Screens which must be/can be closed automatically when the state changes
            // Other screens will stay open and we navigate to the new state-based destination after the
            // current screen is closed
            val currentDestinationAllowsAutoNavigate = listOf(
                R.id.splashFragment,
                R.id.workspaceConnect,
                R.id.workspacePrePrint,
                R.id.workspacePrint,
                R.id.terminalFragment,
                R.id.fileDetailsFragment,
                R.id.loginFragment,
                R.id.fileListFragment,
            ).contains(navController.currentDestination?.id)

            if (currentDestinationAllowsAutoNavigate) {
                viewModel.lastNavigation = id
                navController.navigate(id)
            } else {
                Timber.v("Current destination does not allow auto navigate, storing navigation action as pending")
                viewModel.pendingNavigation = id
            }
        }
    }

    private fun onEventReceived(e: Event) = when (e) {
        // Only show errors if we are not already in disconnected screen. We still want to show the stall warning to indicate something is wrong
        // as this might lead to the user being stuck
        is Event.Disconnected -> {
            Timber.w("Connection lost")
            viewModel.connectionType = null
            when {
                e.exception is WebSocketMaybeBrokenException -> e.exception?.let(this::showDialog)
                e.exception is WebSocketUpgradeFailedException -> e.exception?.let(this::showDialog)
                !listOf(R.id.action_connect_printer, R.id.action_sign_in_required).contains(viewModel.lastNavigation) ->
                    showBanner(R.string.main___banner_connection_lost_reconnecting, 0, R.color.color_error, showSpinner = true, alreadyShrunken = false)
                else -> Unit
            }
        }

        is Event.Connected -> {
            Timber.w("Connection restored")
            viewModel.connectionType = e.connectionType
            updateConnectionBanner(false)
            updateAllWidgets()
        }

        is Event.MessageReceived -> onMessageReceived(e.message)

        else -> Unit
    }

    private fun updateConnectionBanner(alreadyShrunken: Boolean) {
        when (viewModel.connectionType) {
            ConnectionType.Primary -> setBannerVisible(false)
            ConnectionType.Alternative -> showBanner(
                R.string.main___banner_connected_via_alternative,
                R.drawable.ic_round_cloud_queue_24,
                R.color.blue,
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
        }
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
            return Timber.tag("navigation").i("Skipping flag navigation, recently changed and waiting for confirmation")
        }

        navigate(
            when {
                // We encountered an error, try reconnecting
                flags == null || flags.isError() -> {
                    PrintNotificationManager.stop(this)
                    R.id.action_connect_printer
                }

                // We are printing
                flags.isPrinting() -> {
                    try {
                        PrintNotificationManager.start(this)
                    } catch (e: IllegalStateException) {
                        // User might have closed app just in time so we can't start the service
                    }
                    R.id.action_printer_active
                }

                // We are connected
                flags.isOperational() -> {
                    PrintNotificationManager.stop(this)
                    R.id.action_printer_connected
                }

                !flags.isOperational() && !flags.isPrinting() -> {
                    PrintNotificationManager.stop(this)
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

    private fun showBanner(@StringRes text: Int, @DrawableRes icon: Int?, @ColorRes background: Int, showSpinner: Boolean, alreadyShrunken: Boolean) {
        binding.bannerView.show(this, text, icon, background, showSpinner, alreadyShrunken)
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
        lifecycleScope.launchWhenCreated {
            try {
                viewModel.lastSuccessfulCapabilitiesUpdate = System.currentTimeMillis()
                Injector.get().updateInstanceCapabilitiesUseCase().execute(UpdateInstanceCapabilitiesUseCase.Params(updateM115 = updateM115))
                updateAllWidgets()
            } catch (e: Exception) {
                viewModel.lastSuccessfulCapabilitiesUpdate = 0
                if (escalateError) {
                    Timber.e(e)
                    showDialog(getString(R.string.capabilities_validation_error))
                }
            }
        }
    }

    override fun startPrintNotificationService() {
        PrintNotificationManager.start(this)
    }
}