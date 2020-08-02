package de.crysxd.octoapp.base.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
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

    private var errorDialog: AlertDialog? = null

    abstract val octoToolbar: OctoToolbar

    abstract val octo: OctoView

    abstract val coordinatorLayout: CoordinatorLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        instance = this
        super.onCreate(savedInstanceState)
    }

    fun observeErrorEvents(events: LiveData<Event<Throwable>>) = events.observe(this, Observer {
        it.value?.let(this::showErrorDialog)
    })

    fun observerMessageEvents(events: LiveData<Event<(Context) -> CharSequence>>) = events.observe(this, Observer {
        it.value?.let {
            Snackbar.make(coordinatorLayout, it(this), Snackbar.LENGTH_SHORT).show()
        }
    })

    fun showErrorDialog(e: Throwable) = showErrorDialog(
        getString((e as? UserMessageException)?.userMessage ?: R.string.error_general)
    )

    fun showErrorDialog(message: CharSequence) {
        errorDialog?.dismiss()
        errorDialog = AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}