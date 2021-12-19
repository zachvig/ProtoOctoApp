package de.crysxd.baseui.timelapse

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.databinding.TimelapsePlaybackFragmentBinding
import de.crysxd.baseui.di.injectViewModel
import de.crysxd.baseui.ext.requireOctoActivity


class TimelapsePlaybackFragment : Fragment() {
    private lateinit var binding: TimelapsePlaybackFragmentBinding
    private val player by lazy { ExoPlayer.Builder(requireContext()).build() }
    private val viewModel: TimelapsePlaybackViewModel by injectViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        TimelapsePlaybackFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.player.player = player
        player.setMediaItem(MediaItem.fromUri(navArgs<TimelapsePlaybackFragmentArgs>().value.fileUri))
        player.prepare()
        player.seekTo(viewModel.playBackPosition)
    }

    override fun onStart() {
        super.onStart()
        viewModel.requestedOrientationBackup = requireActivity().requestedOrientation
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        requireOctoActivity().octo.isVisible = false
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = requireActivity().window.decorView.systemUiVisibility
            viewModel.systemUiFlagsBackup = flags
            flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            requireActivity().window.decorView.systemUiVisibility = flags
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.playBackPosition = player.currentPosition
        viewModel.requestedOrientationBackup?.let { requireActivity().requestedOrientation = it }
        viewModel.systemUiFlagsBackup?.let { requireActivity().window.decorView.systemUiVisibility = it }
    }
}