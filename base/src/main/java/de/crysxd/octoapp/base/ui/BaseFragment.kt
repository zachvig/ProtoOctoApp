package de.crysxd.octoapp.base.ui

import android.app.AlertDialog
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.R

abstract class BaseFragment(@LayoutRes layout: Int) : Fragment(layout) {

    protected abstract val viewModel: BaseViewModel
    private var errorDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.errorLiveData.observe(this, Observer {
            errorDialog?.dismiss()
            errorDialog = AlertDialog.Builder(requireContext())
                .setMessage(
                    (it as? de.crysxd.octoapp.base.UserMessageException)?.userMessage ?: getString(
                        R.string.error_general)
                )
                .setPositiveButton(android.R.string.ok, null)
                .show()
        })
    }
}