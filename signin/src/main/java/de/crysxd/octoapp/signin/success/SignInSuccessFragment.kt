package de.crysxd.octoapp.signin.success

import android.graphics.Rect
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.ui.base.InsetAwareScreen
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.databinding.SignInSuccessFragmentBinding
import de.crysxd.octoapp.signin.databinding.SignInSuccessFragmentContentBinding

class SignInSuccessFragment : Fragment(), InsetAwareScreen {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            postponeEnterTransition()
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
        binding.buttonContinue.setOnClickListener {
            val args = navArgs<SignInSuccessFragmentArgs>().value
            Injector.get().octorPrintRepository().setActive(
                OctoPrintInstanceInformationV2(
                    webUrl = args.webUrl,
                    apiKey = args.apiKey
                )
            )
        }
        binding.videoMask.animate()
            .setStartDelay(500)
            .alpha(0.75f)
            .setDuration(800)
            .start()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun playVideo() {
        val mediaPath = Uri.parse("android.resource://${requireContext().packageName}/${R.raw.success}")
        mediaPlayer.isLooping = true
        mediaPlayer.setDataSource(requireContext(), mediaPath)
        mediaPlayer.prepareAsync()
        mediaPlayer.playbackParams = mediaPlayer.playbackParams.also { p -> p.speed = 0.6f }
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
