package de.crysxd.octoapp.base.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.models.exceptions.UserMessageException

abstract class BaseFragment(@LayoutRes layout: Int) : Fragment(layout) {

    protected abstract val viewModel: BaseViewModel
    private var errorDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.navContoller = findNavController()
        viewModel.errorLiveData.observe(viewLifecycleOwner, Observer(this::handleError))
        viewModel.messages.observe(viewLifecycleOwner, Observer(this::handleMessage))
    }

    fun handleMessage(generator: (Context) -> CharSequence) {
        Snackbar.make(requireView(), generator(requireContext()), Snackbar.LENGTH_SHORT).show()
    }

    fun handleError(error: Throwable) {
        errorDialog?.dismiss()
        errorDialog = AlertDialog.Builder(requireContext())
            .setMessage(
                getString(
                    (error as? UserMessageException)?.userMessage ?: R.string.error_general
                )
            )
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}