package de.crysxd.octoapp.signin.success

import android.graphics.Rect
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.transition.TransitionInflater
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import de.crysxd.baseui.InsetAwareScreen
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.databinding.SignInSuccessFragmentBinding
import de.crysxd.octoapp.signin.databinding.SignInSuccessFragmentContentBinding
import de.crysxd.octoapp.signin.ext.goBackToDiscover
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.util.UUID

class SignInSuccessFragment : Fragment(), InsetAwareScreen {

    companion object {
        private const val START_DELAY = 500L
    }

    private lateinit var binding: SignInSuccessFragmentBinding
    private val mediaPlayer = MediaPlayer()

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            playVideo()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backgroundSurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
            override fun surfaceCreated(holder: SurfaceHolder) {
                mediaPlayer.setSurface(holder.surface)
            }
        })

        binding.buttonContinue.animate().setStartDelay(START_DELAY * 4).setDuration(600).alpha(1f).start()

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
                    webUrl = UriLibrary.secureDecode(args.webUrl).toHttpUrl(),
                    apiKey = args.apiKey
                )
            )
        }

        // Disable back button, we can't go back here
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goBackToDiscover()
        })
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun playVideo() {
        val mediaPath = Uri.parse(getString(R.string.video_url___success))
        mediaPlayer.isLooping = true
        mediaPlayer.setDataSource(requireContext(), mediaPath)
        mediaPlayer.prepareAsync()
        mediaPlayer.playbackParams = mediaPlayer.playbackParams.also { p -> p.speed = 0.6f }
        mediaPlayer.setOnPreparedListener {
            mediaPlayer.start()
        }
        val loadingStart = System.currentTimeMillis()
        mediaPlayer.setOnInfoListener { _, what, _ ->
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                val delay = (START_DELAY - (System.currentTimeMillis() - loadingStart)).coerceAtLeast(0)
                binding.videoMask.animate()
                    .setStartDelay(delay)
                    .alpha(0.75f)
                    .setDuration(800)
                    .start()
            }

            true
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
