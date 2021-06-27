package de.crysxd.octoapp.signin.access

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.common.NetworkStateViewModel
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.databinding.BaseSigninFragmentBinding
import de.crysxd.octoapp.signin.databinding.ReqestAccessFragmentBinding
import de.crysxd.octoapp.signin.di.injectViewModel
import timber.log.Timber


class RequestAccessFragment : BaseFragment() {
    override val viewModel by injectViewModel<RequestAccessViewModel>()
    private lateinit var binding: BaseSigninFragmentBinding
    private val wifiViewModel by injectViewModel<NetworkStateViewModel>(Injector.get().viewModelFactory())
    private val mediaPlayer = MediaPlayer()
    private lateinit var contentBinding: ReqestAccessFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        BaseSigninFragmentBinding.inflate(layoutInflater, container, false).also {
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.content.removeAllViews()
        contentBinding = ReqestAccessFragmentBinding.inflate(LayoutInflater.from(requireContext()), binding.content, true)
        contentBinding.buttonApiKey.setOnClickListener { continueWithManualApiKey() }

        viewModel.useWebUrl(navArgs<RequestAccessFragmentArgs>().value.webUrl)
        viewModel.uiState.observe(viewLifecycleOwner) {
            when (it) {
                RequestAccessViewModel.UiState.PendingApproval -> Unit
                RequestAccessViewModel.UiState.ManualApiKeyRequired -> continueWithManualApiKey()
                is RequestAccessViewModel.UiState.AccessGranted -> continueWithApiKey(it.apiKey)
            }
        }
        playVideo()

        wifiViewModel.networkState.observe(viewLifecycleOwner) {
            Timber.i("Wifi state: $it")
            binding.wifiWarning.isVisible = it is NetworkStateViewModel.NetworkState.WifiNotConnected
        }
    }

    override fun onResume() {
        super.onResume()
        playVideo()
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
    }

    private fun playVideo() {
        contentBinding.video.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
            override fun surfaceCreated(holder: SurfaceHolder) {
                mediaPlayer.setSurface(holder.surface)
                val mediaPath = Uri.parse("android.resource://${requireContext().packageName}/${R.raw.access_explainer}")
                mediaPlayer.setDataSource(requireContext(), mediaPath)
                mediaPlayer.prepareAsync()
                mediaPlayer.setOnPreparedListener {
                    mediaPlayer.start()
                }
                mediaPlayer.setOnInfoListener { _, what, _ ->
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        startPostponedEnterTransition()
                    }
                    true
                }
            }
        })
    }

    private fun continueWithManualApiKey() {
        Toast.makeText(requireContext(), "Manual API key", Toast.LENGTH_SHORT).show()
    }

    private fun continueWithApiKey(apiKey: String) {
        Toast.makeText(requireContext(), apiKey, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }
}