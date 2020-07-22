package de.crysxd.octoapp.base.feedback

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import kotlinx.android.synthetic.main.fragment_dialog_send_feedback.*
import kotlinx.coroutines.launch
import timber.log.Timber


class SendFeedbackDialog : DialogFragment() {

    private var screenshot: Bitmap? = null
    private val viewModel: SendFeedbackViewModel by injectViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_dialog_send_feedback, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // We take the screenshot before we can access the VM
        // We "cache" the screenshot here and then set it to the VM, falling back to the VM's value
        viewModel.screenshot = screenshot ?: viewModel.screenshot
        if (viewModel.screenshot == null) {
            checkboxScreenshot.isChecked = false
            checkboxScreenshot.isEnabled = false
        }

        buttonOpenEmail.setOnClickListener {
            viewModel.sendFeedback(it.context, checkboxLogs.isChecked, checkboxScreenshot.isChecked)
            dismissAllowingStateLoss()
        }

        // Fix sizing of dialog
        lifecycleScope.launchWhenResumed {
            dialog?.getWindow()?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        // Take screenshot before dialog is shown
        lifecycleScope.launch {
            screenshot = Injector.get().takeScreenshotUseCase().execute(requireOctoActivity())
        }.invokeOnCompletion {
            it?.let { Timber.e(it); requireOctoActivity().showErrorDialog(it) }
            super.show(manager, tag)
        }
    }
}