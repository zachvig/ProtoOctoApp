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
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.models.Event
import de.crysxd.octoapp.base.models.exceptions.UserMessageException
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.common.OctoView

abstract class OctoActivity : AppCompatActivity() {

    private val insetsViewModel: WindowInsetsViewModel by injectViewModel()

    private var errorDialog: AlertDialog? = null

    abstract val octoToolbar: OctoToolbar

    abstract val octo: OctoView

    abstract val coordinatorLayout: CoordinatorLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    fun observeErrorEvents(events: LiveData<Event<Throwable>>) = events.observe(this, Observer {
        it.value?.let {
            errorDialog?.dismiss()
            errorDialog = AlertDialog.Builder(this)
                .setMessage(getString((it as? UserMessageException)?.userMessage ?: R.string.error_general))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    })

    fun observerMessageEvents(events: LiveData<Event<(Context) -> CharSequence>>) = events.observe(this, Observer {
        it.value?.let {
            Snackbar.make(coordinatorLayout, it(this), Snackbar.LENGTH_SHORT).show()
        }
    })
}