package de.crysxd.octoapp.signin.access

import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.common.NetworkStateViewModel
import de.crysxd.baseui.di.BaseUiInjector
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.databinding.BaseSigninFragmentBinding
import de.crysxd.octoapp.signin.databinding.ReqestAccessFragmentBinding
import de.crysxd.octoapp.signin.di.injectViewModel
import de.crysxd.octoapp.signin.ext.goBackToDiscover
import de.crysxd.octoapp.signin.ext.setUpAsHelpButton
import timber.log.Timber


class RequestAccessFragment : BaseFragment() {
    override val viewModel by injectViewModel<RequestAccessViewModel>()
    private lateinit var binding: BaseSigninFragmentBinding
    private val wifiViewModel by injectViewModel<NetworkStateViewModel>(BaseUiInjector.get().viewModelFactory())
    private lateinit var contentBinding: ReqestAccessFragmentBinding
    private val webUrl get() = UriLibrary.secureDecode(navArgs<RequestAccessFragmentArgs>().value.webUrl)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.sign_in_shard_element)
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
        contentBinding.openWeb.setOnClickListener { viewModel.openInWeb(webUrl) }
        setUpAsHelpButton(contentBinding.help)

        viewModel.useWebUrl(webUrl)
        viewModel.uiState.observe(viewLifecycleOwner) {
            when (it) {
                RequestAccessViewModel.UiState.PendingApproval -> Unit
                RequestAccessViewModel.UiState.ManualApiKeyRequired -> continueWithManualApiKey()
                is RequestAccessViewModel.UiState.AccessGranted -> continueWithApiKey(it.apiKey)
            }
        }
        prepareVideo()

        wifiViewModel.networkState.observe(viewLifecycleOwner) {
            Timber.i("Wifi state: $it")
            binding.wifiWarning.isVisible = it is NetworkStateViewModel.NetworkState.WifiNotConnected
        }

        // Disable back button, we can't go back here
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBackToDiscover()
        })

        // Case A: We got here because a API key was invalid. In this case we allow the user to go back to discover to connect an other OctoPrint
        contentBinding.buttonConnectOther.setOnClickListener { goBackToDiscover() }
        contentBinding.buttonConnectOther.isVisible = BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot() != null
    }

    override fun onPause() {
        super.onPause()
        contentBinding.videoOverlay.alpha = 1f
    }

    override fun onResume() {
        super.onResume()
        requireOctoActivity().octo.isVisible = false
        contentBinding.video.start()
    }

    private fun prepareVideo() {
        contentBinding.videoOverlay.alpha = 1f
        Timber.v("Preparing video")
        BaseInjector.get().mediaFileRepository().getMediaUri(getString(R.string.video_url___access_explainer), viewLifecycleOwner) { uri ->
            Timber.v("Uri ready: $uri")
            contentBinding.video.setVideoURI(uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Set this BEFORE start playback
                contentBinding.video.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
            }

            contentBinding.video.setOnPreparedListener {
                it.isLooping = true
            }

            contentBinding.video.setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    Timber.v("Removing overlay")
                    contentBinding.videoOverlay.animate().alpha(0f).setDuration(300).start()
                }
                true
            }

            contentBinding.video.start()
        }
    }

    private fun continueWithManualApiKey() {
        val extras = FragmentNavigatorExtras(binding.octoView to "octoView", binding.octoBackground to "octoBackground")
        val directions = RequestAccessFragmentDirections.actionManuallyEnterApiKey(webUrl)
        findNavController().navigate(directions, extras)
    }

    private fun continueWithApiKey(apiKey: String) {
        val extras = FragmentNavigatorExtras(binding.octoView to "octoView", binding.octoBackground to "octoBackground")
        val directions = RequestAccessFragmentDirections.actionSuccess(webUrl = UriLibrary.secureEncode(webUrl), apiKey = apiKey)
        findNavController().navigate(directions, extras)
    }
}