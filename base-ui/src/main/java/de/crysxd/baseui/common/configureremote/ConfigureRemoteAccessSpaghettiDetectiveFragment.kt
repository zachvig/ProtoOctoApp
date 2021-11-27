package de.crysxd.baseui.common.configureremote

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.transition.TransitionManager
import de.crysxd.baseui.R
import de.crysxd.baseui.databinding.ConfigureRemoteAccessSpaghettiDetectiveFragmentBinding
import de.crysxd.baseui.di.injectParentViewModel
import de.crysxd.baseui.di.injectViewModel
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.octoprint.isSpaghettiDetectiveUrl
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ConfigureRemoteAccessSpaghettiDetectiveFragment : Fragment() {

    private val viewModel by injectParentViewModel<ConfigureRemoteAccessViewModel>()
    private val tsdViewModel by injectViewModel<ConfigureRemoteAccessSpaghettiDetectiveViewModel>()
    private lateinit var binding: ConfigureRemoteAccessSpaghettiDetectiveFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ConfigureRemoteAccessSpaghettiDetectiveFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.connectTsd.setOnClickListener {
            viewModel.getSpaghettiDetectiveSetupUrl()
        }

        binding.disconnectTsd.setOnClickListener {
            viewModel.setRemoteUrl("", "", "", false)
        }

        binding.reloadDataUsage.setOnClickListener { tsdViewModel.fetchDataUsage() }

        viewModel.viewData.observe(viewLifecycleOwner) {
            val tsdConnected = it.remoteWebUrl != null && it.remoteWebUrl.isSpaghettiDetectiveUrl()
            binding.connected.isVisible = tsdConnected
            binding.disconnected.isVisible = !binding.connected.isVisible

            if (tsdConnected) {
                tsdViewModel.fetchDataUsage()
            }
        }

        binding.dataUsageBar.max = 100
        tsdViewModel.dataUsage.observe(viewLifecycleOwner) {
            when (it) {
                is ConfigureRemoteAccessSpaghettiDetectiveViewModel.DataUsageWrapper.Data -> {
                    binding.dataUsageBar.post {
                        binding.dataUsageBar.isIndeterminate = false
                        binding.dataUsageBar.progress = ((it.dataUsage.totalBytes / it.dataUsage.monthlyCapBytes.toFloat()) * binding.dataUsageBar.max).toInt()
                        Timber.i("max=${binding.dataUsageBar.max} progress=${binding.dataUsageBar.progress}")
                    }

                    val resetInMillis = TimeUnit.SECONDS.toMillis(it.dataUsage.resetInSeconds.toLong())
                    val resetDate = System.currentTimeMillis() + resetInMillis
                    val relativeString = DateUtils.getRelativeTimeSpanString(resetDate, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)

                    binding.dataUsage.isVisible = true
                    binding.dataUsage.text = getString(
                        R.string.configure_remote_acces___spaghetti_detective___data_usage_limited,
                        it.dataUsage.totalBytes.toLong().asStyleFileSize(),
                        it.dataUsage.monthlyCapBytes.toLong().asStyleFileSize(),
                        relativeString
                    )
                }

                ConfigureRemoteAccessSpaghettiDetectiveViewModel.DataUsageWrapper.Failed -> {
                    binding.dataUsageBar.post {
                        binding.dataUsageBar.isIndeterminate = false
                        binding.dataUsageBar.progress = 0
                    }
                    binding.dataUsage.isVisible = true
                    binding.dataUsage.setText(R.string.configure_remote_acces___spaghetti_detective___data_usage_failed)
                }

                ConfigureRemoteAccessSpaghettiDetectiveViewModel.DataUsageWrapper.Loading -> {
                    binding.dataUsageBar.post {
                        binding.dataUsageBar.isIndeterminate = true
                    }

                    binding.dataUsage.isInvisible = true
                }
            }
        }
    }
}