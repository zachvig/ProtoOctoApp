package de.crysxd.baseui.purchase

import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.baseui.R
import de.crysxd.baseui.databinding.PurchaseCofirmationDialogBinding
import de.crysxd.octoapp.base.di.BaseInjector
import kotlinx.coroutines.delay

class PurchaseConfirmationDialog : DialogFragment() {
    private val mediaPlayer = MediaPlayer()
    private lateinit var binding: PurchaseCofirmationDialogBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        PurchaseCofirmationDialogBinding.inflate(inflater, container, false).also { binding = it }.root

    private fun prepareVideo() {
        val loadingStart = System.currentTimeMillis()
        BaseInjector.get().mediaFileRepository().getMediaUri(getString(R.string.video_url___success), viewLifecycleOwner) { uri ->
            binding.backgroundSurface.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
                override fun surfaceDestroyed(holder: SurfaceHolder) = Unit
                override fun surfaceCreated(holder: SurfaceHolder) {
                    mediaPlayer.setDataSource(requireContext(), uri)
                    mediaPlayer.setDisplay(holder)
                    mediaPlayer.prepareAsync()
                    mediaPlayer.isLooping = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mediaPlayer.playbackParams = mediaPlayer.playbackParams.also { p -> p.speed = 0.6f }
                    }

                    startPostponedEnterTransition()

                    mediaPlayer.setOnPreparedListener {
                        mediaPlayer.start()
                    }
                    mediaPlayer.setOnInfoListener { _, what, _ ->
                        if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            binding.backgroundSurfaceOverlay.animate()
                                .setDuration(600)
                                .alpha(0.75f)
                                .start()
                        }

                        true
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.button.setOnClickListener {
            dismiss()
        }
        val email = Firebase.remoteConfig.getString("contact_email")
        binding.content.text = HtmlCompat.fromHtml(
            getString(R.string.purchase_dialog_text, "<a href=\"mailto:$email\">$email</a>"),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        binding.content.movementMethod = LinkMovementMethod()
        binding.header.imageViewStatusBackground.isVisible = false
        binding.backgroundSurface.holder.setFormat(PixelFormat.TRANSLUCENT)

        // Fade views in
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val views = listOf(binding.textView4, binding.textView3, binding.content, binding.button)
            views.forEach { it.alpha = 0f }
            delay(1000)
            views.forEach {
                it.animate().alpha(1f).also { it.duration = 700 }.start()
                delay(200)
            }
        }

        prepareVideo()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}