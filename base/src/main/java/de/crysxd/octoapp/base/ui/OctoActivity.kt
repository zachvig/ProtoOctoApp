package de.crysxd.octoapp.base.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ext.composeErrorMessage
import de.crysxd.octoapp.base.ext.composeMessageStack
import de.crysxd.octoapp.base.feedback.SendFeedbackDialog
import de.crysxd.octoapp.base.models.Event
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.common.OctoView
import kotlinx.coroutines.CancellationException
import timber.log.Timber

abstract class OctoActivity : LocaleActivity() {

    internal companion object {
        lateinit var instance: OctoActivity
            private set
    }

    private var dialog: AlertDialog? = null

    abstract val octoToolbar: OctoToolbar

    abstract val octo: OctoView

    abstract val coordinatorLayout: CoordinatorLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        instance = this
        super.onCreate(savedInstanceState)
    }

    fun observeErrorEvents(events: LiveData<Event<Throwable>>) = events.observe(this) {
        it.value?.let(this::showDialog)
    }

    fun observerMessageEvents(events: LiveData<Event<BaseViewModel.Message>>) = events.observe(this) { event ->
        when (val message = event.value) {
            null -> Unit
            is BaseViewModel.Message.SnackbarMessage -> showSnackbar(message)
            is BaseViewModel.Message.DialogMessage -> showDialog(message)
        }.toString()
    }

    private fun showSnackbar(message: BaseViewModel.Message.SnackbarMessage) {
        Snackbar.make(coordinatorLayout, message.text(this), message.duration).apply {
            message.actionText(this@OctoActivity)?.let {
                setAction(it) { message.action(this@OctoActivity) }
            }

            setBackgroundTint(
                ContextCompat.getColor(
                    this@OctoActivity, when (message.type) {
                        BaseViewModel.Message.SnackbarMessage.Type.Neutral -> R.color.snackbar_neutral
                        BaseViewModel.Message.SnackbarMessage.Type.Positive -> R.color.snackbar_positive
                        BaseViewModel.Message.SnackbarMessage.Type.Negative -> R.color.snackbar_negative
                    }
                )
            )

            val foregroundColor = ContextCompat.getColor(this@OctoActivity, R.color.text_colored_background)
            setTextColor(foregroundColor)
            setActionTextColor(foregroundColor)
        }.show()
    }

    private fun showDialog(message: BaseViewModel.Message.DialogMessage) {
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
            SendFeedbackDialog().show(supportFragmentManager, "get-support")
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
    ) {
        Timber.i("Showing dialog: [message=$message, positiveButton=$positiveButton, neutralButton=$neutralButton")
        dialog?.dismiss()
        dialog = MaterialAlertDialogBuilder(this).let { builder ->
            builder.setMessage(message)
            builder.setPositiveButton(positiveButton) { _, _ -> positiveAction(this) }
            neutralButton?.let {
                builder.setNeutralButton(it) { _, _ -> neutralAction(this) }
            }
            builder.show()
        }
    }

    abstract fun startPrintNotificationService()
}