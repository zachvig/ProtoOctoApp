package de.crysxd.octoapp.base.billing

import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.R
import kotlinx.android.synthetic.main.dialog_purchase_cofirmation.*
import kotlinx.android.synthetic.main.purchase_header.*
import kotlinx.coroutines.delay
import timber.log.Timber

class PurchaseConfirmationDialog : DialogFragment() {

    private val mediaPlayer = MediaPlayer()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.dialog_purchase_cofirmation, container, false)

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaPlayer.isLooping = true
        Firebase.remoteConfig.getString("purchase_confirmation_background_video").takeIf { it.isNotBlank() }?.let {
            mediaPlayer.setDataSource(it)
            mediaPlayer.prepareAsync()
            mediaPlayer.playbackParams = mediaPlayer.playbackParams.also { p -> p.speed = 0.8f }
            mediaPlayer.setOnPreparedListener {
                mediaPlayer.start()
            }
            mediaPlayer.setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    Timber.i("Fading overlay out")
                    backgroundSurfaceOverlay?.animate()?.alpha(0.9f)?.start()
                }

                true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button.setOnClickListener {
            dismiss()
        }
        val email = Firebase.remoteConfig.getString("contact_email")
        content.text = HtmlCompat.fromHtml(
            getString(R.string.purchase_dialog_text, "<a href=\"mailto:$email\">$email</a>"),
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        content.movementMethod = LinkMovementMethod()
        imageViewStatusBackground.isVisible = false
        backgroundSurface.holder.setFormat(PixelFormat.TRANSLUCENT)
        backgroundSurface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) = Unit
            override fun surfaceDestroyed(holder: SurfaceHolder?) = Unit
            override fun surfaceCreated(holder: SurfaceHolder) {
                mediaPlayer.setSurface(holder.surface)
            }
        })

        // Fade views in
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val views = listOf(textView4, textView3, content, button)
            views.forEach { it.alpha = 0f }
            delay(1000)
            views.forEach {
                it.animate().alpha(1f).also { it.duration = 700 }.start()
                delay(200)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}