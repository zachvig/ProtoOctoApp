package de.crysxd.octoapp.signin.success

import android.graphics.Rect
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
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import de.crysxd.baseui.InsetAwareScreen
import de.crysxd.baseui.common.ScalableVideoView
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.databinding.SignInSuccessFragmentBinding
import de.crysxd.octoapp.signin.databinding.SignInSuccessFragmentContentBinding
import de.crysxd.octoapp.signin.ext.goBackToDiscover
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import java.util.UUID

class SignInSuccessFragment : Fragment(), InsetAwareScreen {

    companion object {
        private const val START_DELAY = 2000L
        private const val DURATION = 600L
    }

    private lateinit var binding: SignInSuccessFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        SignInSuccessFragmentBinding.inflate(layoutInflater, container, false).also {
            it.base.content.removeAllViews()
            SignInSuccessFragmentContentBinding.inflate(inflater, it.base.content, true)
            binding = it
        }.root

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(context).inflateTransition(R.transition.sign_in_shard_element)
        sharedElementReturnTransition = TransitionInflater.from(context).inflateTransition(R.transition.sign_in_shard_element)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonContinue.animate().setStartDelay(START_DELAY).setDuration(DURATION).alpha(1f).start()
        binding.base.octoView.scheduleAnimation(1000) {
            party()
        }

        binding.buttonContinue.setOnClickListener {
            val args = navArgs<SignInSuccessFragmentArgs>().value
            OctoAnalytics.logEvent(OctoAnalytics.Event.Login)
            OctoAnalytics.logEvent(OctoAnalytics.Event.SignInSuccess)

            // Clearing and setting the active will enforce the navigation to be reset
            // This is important in case we got here after a API key was invalid
            BaseInjector.get().octorPrintRepository().clearActive()
            BaseInjector.get().octorPrintRepository().setActive(
                OctoPrintInstanceInformationV3(
                    id = UUID.randomUUID().toString(),
                    webUrl = UriLibrary.secureDecode(args.webUrl).toHttpUrlOrNull().let {
                        it ?: throw IllegalStateException("Not an HTTP url: ${args.webUrl} -> ${UriLibrary.secureDecode(args.webUrl)} -> null")
                    },
                    apiKey = args.apiKey
                )
            )
        }

        // Disable back button, we can't go back here
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBackToDiscover()
        })

        prepareVideo()
    }

    override fun onPause() {
        super.onPause()
        binding.videoOverlay.alpha = 1f
    }

    override fun onResume() {
        super.onResume()
        requireOctoActivity().octo.isVisible = false
        binding.video.start()
    }

    private fun prepareVideo() {
        binding.videoOverlay.alpha = 1f
        val loadingStart = System.currentTimeMillis()
        Timber.v("Preparing video")
        BaseInjector.get().mediaFileRepository().getMediaUri(getString(R.string.video_url___success), viewLifecycleOwner) { uri ->
            Timber.v("Uri ready: $uri")
            binding.video.setVideoURI(uri)
            binding.video.setDisplayMode(ScalableVideoView.DisplayMode.ZOOM)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Set this BEFORE start playback
                binding.video.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
            }
            binding.video.setOnPreparedListener {
                it.isLooping = true
                binding.video.changeVideoSize(it.videoWidth, it.videoHeight)
            }

            binding.video.setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    Timber.v("Removing overlay")
                    val delay = (START_DELAY - (System.currentTimeMillis() - loadingStart)).coerceAtLeast(0)
                    binding.videoOverlay.animate()
                        .setStartDelay(delay)
                        .alpha(0.75f)
                        .setDuration(DURATION)
                        .setStartDelay(delay)
                        .start()
                }
                true
            }

            binding.video.start()
        }
    }

    override fun handleInsets(insets: Rect) {
        binding.base.root.updatePadding(
            left = insets.left,
            right = insets.right,
            top = insets.top,
            bottom = insets.bottom
        )
        binding.buttonContainer.updatePadding(
            left = insets.left,
            right = insets.right,
            top = insets.top,
            bottom = insets.bottom
        )
    }
}
