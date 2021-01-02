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

        viewModel.viewState.observe(viewLifecycleOwner) {
            when (it) {
                SendFeedbackViewModel.ViewState.Idle -> Unit
                SendFeedbackViewModel.ViewState.Loading -> {
                    buttonOpenEmail.isEnabled = false
                    buttonOpenEmail.setText(R.string.loading)
                }
                SendFeedbackViewModel.ViewState.Done -> dismissAllowingStateLoss()
            }
        }

        buttonOpenEmail.setOnClickListener {
            messageInput.error = if (messageInput.editText!!.text.isEmpty()) {
                getString(R.string.error_please_enter_a_message)
            } else {
                viewModel.sendFeedback(
                    context = it.context,
                    message = messageInput.editText!!.text.toString(),
                    sendPhoneInfo = checkboxPhoneInformation.isChecked,
                    sendOctoPrintInfo = checkboxOctoprintInformation.isChecked,
                    sendLogs = checkboxLogs.isChecked,
                    sendScreenshot = checkboxScreenshot.isChecked
                )
                null
            }
        }

        // Fix sizing of dialog
        lifecycleScope.launchWhenResumed {
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        // Take screenshot before dialog is shown
        lifecycleScope.launch {
            try {
                screenshot = Injector.get().takeScreenshotUseCase().execute(requireOctoActivity())
            } catch (e: Exception) {
                Timber.e(e)
            }
            super.show(manager, tag)
        }
    }
}