package de.crysxd.octoapp.signin.access

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.common.NetworkStateViewModel
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.databinding.BaseSigninFragmentBinding
import de.crysxd.octoapp.signin.databinding.ReqestAccessFragmentBinding
import de.crysxd.octoapp.signin.di.injectViewModel
import de.crysxd.octoapp.signin.discover.DiscoverViewModel
import timber.log.Timber


class RequestAccessFragment : BaseFragment() {
    override val viewModel by injectViewModel<DiscoverViewModel>()
    private lateinit var binding: BaseSigninFragmentBinding
    private val wifiViewModel by injectViewModel<NetworkStateViewModel>(Injector.get().viewModelFactory())
    private val mediaPlayer = MediaPlayer()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        BaseSigninFragmentBinding.inflate(layoutInflater, container, false).also {
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.content.removeAllViews()
        val contentBinding = ReqestAccessFragmentBinding.inflate(LayoutInflater.from(requireContext()), binding.content, true)
        playVideo(contentBinding.video, contentBinding.videoContainer)


        wifiViewModel.networkState.observe(viewLifecycleOwner) {
            Timber.i("Wifi state: $it")
            binding.wifiWarning.isVisible = it is NetworkStateViewModel.NetworkState.WifiNotConnected
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
    }

    private fun playVideo(surface: SurfaceView, container: View) {
        surface.alpha = 0f
        surface.holder.addCallback(object : SurfaceHolder.Callback {
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
                        surface.animate().alpha(1f).start()
                    }
                    true
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

}