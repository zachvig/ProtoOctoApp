package de.crysxd.octoapp.base.feedback

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.SendFeedbackDialogBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.DateFormat
import java.util.*


class SendFeedbackDialog : DialogFragment() {

    private lateinit var binding: SendFeedbackDialogBinding
    private var screenshot: Bitmap? = null
    private val viewModel: SendFeedbackViewModel by injectViewModel()

    companion object {
        private const val ARG_FOR_BUG_REPORT = "forBug"

        fun create(isForBugReport: Boolean = false) = SendFeedbackDialog().also {
            it.arguments = bundleOf(ARG_FOR_BUG_REPORT to isForBugReport)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        SendFeedbackDialogBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // We take the screenshot before we can access the VM
        // We "cache" the screenshot here and then set it to the VM, falling back to the VM's value
        viewModel.screenshot = screenshot ?: viewModel.screenshot
        if (viewModel.screenshot == null) {
            binding.checkboxScreenshot.isChecked = false
            binding.checkboxScreenshot.isEnabled = false
        }

        val isForBugReport = arguments?.getBoolean(ARG_FOR_BUG_REPORT, false) == true
        if (isForBugReport) {
            binding.checkboxPhoneInformation.isChecked = true
            binding.checkboxPhoneInformation.isEnabled = false
            binding.checkboxOctoprintInformation.isChecked = true
            binding.checkboxOctoprintInformation.isEnabled = false
        }

        try {
            val tz = Firebase.remoteConfig.getString("contact_timezone")
            val time = Calendar.getInstance(TimeZone.getTimeZone(tz)).time
            val formattedTime = DateFormat.getTimeInstance(DateFormat.SHORT).format(time)
            binding.contactTime.text = getString(R.string.help___contact_detail_information, formattedTime, tz)
        } catch (e: java.lang.Exception) {
            Timber.e(e)
            binding.contactTime.isVisible = false
        }

        viewModel.viewState.observe(viewLifecycleOwner) {
            when (it) {
                SendFeedbackViewModel.ViewState.Idle -> Unit
                SendFeedbackViewModel.ViewState.Loading -> {
                    binding.buttonOpenEmail.isEnabled = false
                    binding.buttonOpenEmail.setText(R.string.loading)
                }
                SendFeedbackViewModel.ViewState.Done -> dismissAllowingStateLoss()
            }
        }

        binding.buttonOpenEmail.setOnClickListener {
            binding.messageInput.error = if (binding.messageInput.editText!!.text.isEmpty()) {
                getString(R.string.error_please_enter_a_message)
            } else {
                viewModel.sendFeedback(
                    context = it.context,
                    message = binding.messageInput.editText!!.text.toString(),
                    sendPhoneInfo = binding.checkboxPhoneInformation.isChecked,
                    sendOctoPrintInfo = binding.checkboxOctoprintInformation.isChecked,
                    sendLogs = binding.checkboxLogs.isChecked,
                    sendScreenshot = binding.checkboxScreenshot.isChecked
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