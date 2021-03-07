package de.crysxd.octoapp.base.ui.base

import android.content.Context
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
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ext.composeErrorMessage
import de.crysxd.octoapp.base.ext.composeMessageStack
import de.crysxd.octoapp.base.models.Event
import de.crysxd.octoapp.base.ui.common.LinkClickMovementMethod
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.common.OctoView
import de.crysxd.octoapp.base.ui.widget.OctoWidgetRecycler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import timber.log.Timber

@Suppress("EXPERIMENTAL_API_USAGE")
abstract class OctoActivity : LocalizedActivity() {

    internal companion object {
        lateinit var instance: OctoActivity
            private set
    }

    val octoWidgetRecycler = OctoWidgetRecycler()
    private var dialog: AlertDialog? = null
    abstract val octoToolbar: OctoToolbar
    abstract val octo: OctoView
    abstract val rootLayout: FrameLayout
    abstract val navController: NavController
    private val handler = Handler(Looper.getMainLooper())
    private val snackbarMessageChannel = ConflatedBroadcastChannel<Message.SnackbarMessage?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        instance = this
        super.onCreate(savedInstanceState)

        // Debounce snackbars to prevent them from "flashing up"
        lifecycleScope.launchWhenCreated {
            snackbarMessageChannel.asFlow()
                .debounce(1000)
                .filterNotNull()
                .collect(::doShowSnackbar)
        }
    }

    fun observeErrorEvents(events: LiveData<Event<Throwable>>) = events.observe(this) {
        it.value?.let(this::showDialog)
    }

    fun observerMessageEvents(events: LiveData<Event<Message>>) = events.observe(this) { event ->
        when (val message = event.value) {
            null -> Unit
            is Message.SnackbarMessage -> showSnackbar(message)
            is Message.DialogMessage -> showDialog(message)
        }
    }

    fun showSnackbar(message: Message.SnackbarMessage) {
        if (message.debounce) {
            snackbarMessageChannel.offer(message)
        } else {
            doShowSnackbar(message)
        }
    }

    private fun doShowSnackbar(message: Message.SnackbarMessage) = handler.post {
        snackbarMessageChannel.offer(null)
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
            navController.navigate(R.id.action_help)
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
        neutralAction: (Context) -> Unit = {}
    ) = handler.post {
        // Check activity state before showing dialog
        if (lifecycle.currentState >= Lifecycle.State.CREATED) {
            // If message is aplain string, format with HTML
            val formattedMessage = (message as? String)?.let { HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_COMPACT) } ?: message

            Timber.i("Showing dialog: [message=$message, positiveButton=$positiveButton, neutralButton=$neutralButton")
            dialog?.dismiss()
            dialog = MaterialAlertDialogBuilder(this).let { builder ->
                builder.setMessage(formattedMessage)
                builder.setPositiveButton(positiveButton) { _, _ -> positiveAction(this) }
                neutralButton?.let {
                    builder.setNeutralButton(it) { _, _ -> neutralAction(this) }
                }
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