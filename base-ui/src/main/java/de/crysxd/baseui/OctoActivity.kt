package de.crysxd.baseui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import de.crysxd.baseui.common.LinkClickMovementMethod
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.common.OctoView
import de.crysxd.baseui.widget.OctoWidgetRecycler
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.data.models.Event
import de.crysxd.octoapp.base.ext.composeErrorMessage
import de.crysxd.octoapp.base.ext.composeMessageStack
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.utils.ExceptionReceivers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import timber.log.Timber

abstract class OctoActivity : LocalizedActivity() {

    companion object {
        var instance: OctoActivity? = null
            private set
    }

    val octoWidgetRecycler = OctoWidgetRecycler()
    private var dialog: AlertDialog? = null
    private var dialogHasHighPriority = false
    abstract val octoToolbar: OctoToolbar
    abstract val octo: OctoView
    abstract val rootLayout: FrameLayout
    abstract val navController: NavController
    private val handler = Handler(Looper.getMainLooper())
    private val snackbarMessageChannel = MutableStateFlow<Message.SnackbarMessage?>(null)
    val resultFlow = MutableStateFlow<Triple<Int, Int, Intent>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        instance = this
        super.onCreate(savedInstanceState)

        // Debounce snackbars to prevent them from "flashing up"
        lifecycleScope.launchWhenCreated {
            snackbarMessageChannel
                .debounce(300)
                .filterNotNull()
                .collect(::doShowSnackbar)
        }

        // Register as ExceptionReceiver
        ExceptionReceivers.registerReceiver(this, ::showDialog)
    }

    override fun onStart() {
        super.onStart()
        // Remove splash background
        window.decorView.background = ColorDrawable(ContextCompat.getColor(this, R.color.window_background))
        octo.animateVisibility = true
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    fun observeErrorEvents(events: LiveData<Event<Throwable>>) = events.observe(this) {
        it.value?.let(this::showDialog)
    }

    abstract fun enforceAllowAutomaticNavigationFromCurrentDestination()

    fun observerMessageEvents(events: LiveData<Event<Message>>) = events.observe(this) { event ->
        when (val message = event.value) {
            null -> Unit
            is Message.SnackbarMessage -> showSnackbar(message)
            is Message.DialogMessage -> showDialog(message)
        }
    }

    fun showSnackbar(message: Message.SnackbarMessage) {
        if (message.debounce) {
            snackbarMessageChannel.value = message
        } else {
            doShowSnackbar(message)
        }
    }

    private fun doShowSnackbar(message: Message.SnackbarMessage) = handler.post {
        snackbarMessageChannel.value = null
        Snackbar.make(rootLayout, message.text(this), message.duration).apply {
            message.actionText(this@OctoActivity)?.let {
                setAction(it) { message.action(this@OctoActivity) }
            }

            setBackgroundTint(
                ContextCompat.getColor(
                    this@OctoActivity, when (message.type) {
                        Message.SnackbarMessage.Type.Neutral -> R.color.snackbar_neutral
                        Message.SnackbarMessage.Type.Positive -> R.color.snackbar_positive
                        Message.SnackbarMessage.Type.Negative -> R.color.snackbar_negative
                    }
                )
            )

            val foregroundColor = ContextCompat.getColor(this@OctoActivity, R.color.text_colored_background)
            setTextColor(foregroundColor)
            setActionTextColor(foregroundColor)
        }.also {
            it.view.translationY = octoToolbar.top.toFloat()
            it.view.updateLayoutParams<FrameLayout.LayoutParams> {
                gravity = Gravity.TOP
            }
        }.show()
    }

    private fun showDialog(message: Message.DialogMessage) {
        showDialog(
            message = message.text(this),
            positiveButton = message.positiveButton(this),
            positiveAction = message.positiveAction,
            neutralButton = message.neutralButton(this),
            neutralAction = message.neutralAction
        )
    }

    fun showDialog(e: Throwable) {
        // Safeguard that we don't show an error for cancellation exceptions
        if (e !is CancellationException) {
            Timber.e(e)
            showDialog(
                message = e.composeErrorMessage(this),
                neutralAction = { showErrorDetailsDialog(e) },
                neutralButton = getString(R.string.show_details)
            )
        }
    }

    fun showErrorDetailsDialog(e: Throwable, offerSupport: Boolean = true) = showDialog(
        message = e.composeMessageStack(),
        neutralAction = {
            OctoAnalytics.logEvent(OctoAnalytics.Event.SupportFromErrorDetails)
            UriLibrary.getHelpUri().open(this)
        },
        neutralButton = if (offerSupport) {
            getString(R.string.get_support)
        } else {
            null
        }
    )

    fun showDialog(
        message: CharSequence,
        positiveButton: CharSequence = getString(android.R.string.ok),
        positiveAction: (Context) -> Unit = {},
        neutralButton: CharSequence? = null,
        neutralAction: (Context) -> Unit = {},
        negativeButton: CharSequence? = null,
        negativeAction: (Context) -> Unit = {},
        highPriority: Boolean = false,
    ) = handler.post {
        // Check activity state before showing dialog
        if (lifecycle.currentState >= Lifecycle.State.CREATED) {
            // If message is aplain string, format with HTML
            val formattedMessage = (message as? String)?.let { HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_COMPACT) } ?: message

            if (dialog?.isShowing == true && dialogHasHighPriority) {
                // A high priority dialog is visible, we can't overrule this
                return@post
            }

            Timber.i("Showing dialog: [message=$message, positiveButton=$positiveButton, neutralButton=$neutralButton")
            dialog?.dismiss()
            dialogHasHighPriority = highPriority
            dialog = MaterialAlertDialogBuilder(this).let { builder ->
                builder.setMessage(formattedMessage)
                builder.setPositiveButton(positiveButton) { _, _ -> positiveAction(this) }
                neutralButton?.let {
                    builder.setNeutralButton(it) { _, _ -> neutralAction(this) }
                }
                negativeButton?.let {
                    builder.setNegativeButton(it) { _, _ -> negativeAction(this) }
                }
                builder.setCancelable(!highPriority)
                builder.show().also {
                    // Allow links to be clicked
                    it.findViewById<TextView>(android.R.id.message)?.movementMethod =
                        LinkClickMovementMethod(LinkClickMovementMethod.OpenWithIntentLinkClickedListener(this))
                }
            }
        } else {
            Timber.w("Dialog skipped, activity finished")
        }
    }

    abstract fun startPrintNotificationService()

    sealed class Message {
        data class SnackbarMessage(
            val duration: Int = Snackbar.LENGTH_SHORT,
            val type: Type = Type.Neutral,
            val actionText: (Context) -> CharSequence? = { null },
            val action: (Context) -> Unit = {},
            val debounce: Boolean = false,
            val text: (Context) -> CharSequence
        ) : Message() {
            sealed class Type {
                object Neutral : Type()
                object Positive : Type()
                object Negative : Type()
            }
        }

        data class DialogMessage(
            val text: (Context) -> CharSequence,
            val positiveButton: (Context) -> CharSequence = { it.getString(android.R.string.ok) },
            val neutralButton: (Context) -> CharSequence? = { null },
            val positiveAction: (Context) -> Unit = {},
            val neutralAction: (Context) -> Unit = {}
        ) : Message()
    }
}