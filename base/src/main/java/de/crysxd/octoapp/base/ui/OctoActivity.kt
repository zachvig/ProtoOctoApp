package de.crysxd.octoapp.base.ui

import android.app.AlertDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.models.Event
import de.crysxd.octoapp.base.models.exceptions.UserMessageException
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.common.OctoView

abstract class OctoActivity : AppCompatActivity() {

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

    fun observeErrorEvents(events: LiveData<Event<Throwable>>) = events.observe(this, Observer {
        it.value?.let(this::showDialog)
    })

    fun observerMessageEvents(events: LiveData<Event<BaseViewModel.Message>>) = events.observe(this, Observer { event ->
        when (val message = event.value) {
            null -> Unit
            is BaseViewModel.Message.SnackbarMessage -> showSnackbar(message)
            is BaseViewModel.Message.DialogMessage -> showDialog(message)
        }.toString()
    })

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
        showDialog(message.text(this))
    }

    fun showDialog(e: Throwable) = showDialog(
        getString((e as? UserMessageException)?.userMessage ?: R.string.error_general)
    )

    fun showDialog(message: CharSequence) {
        dialog?.dismiss()
        dialog = AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}