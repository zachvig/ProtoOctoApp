package de.crysxd.octoapp.filemanager.ui.select_file

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.filemanager.R
import de.crysxd.octoapp.filemanager.databinding.SelectFileFragmentBinding
import de.crysxd.octoapp.filemanager.di.injectViewModel
import de.crysxd.octoapp.filemanager.menu.FileActionsMenu
import kotlinx.coroutines.delay
import timber.log.Timber


class SelectFileFragment : BaseFragment(), FileActionsMenu.Callback {

    companion object {
        // Delay the initial loading display a little. Usually we are on fast local networks so the
        // loader would just flash up for a split second which doesn't look nice
        const val LOADER_DELAY = 400L
    }

    private lateinit var binding: SelectFileFragmentBinding
    override val viewModel: SelectFileViewModel by injectViewModel()
    private val navArgs by navArgs<SelectFileFragmentArgs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        SelectFileFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup adapter
        val adapter = SelectFileAdapter(
            onFileSelected = {
                viewModel.selectFile(it)
            },
            onFileMenuOpened = {
                MenuBottomSheetFragment.createForMenu(FileActionsMenu(it)).show(childFragmentManager)
            },
            onHideThumbnailHint = {
                viewModel.hideThumbnailHint()
            },
            onShowThumbnailInfo = {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(getString(R.string.thumbnail_info_message))
                    .setPositiveButton(R.string.cura_plugin) { _, _ ->
                        openLink("https://plugins.octoprint.org/plugins/UltimakerFormatPackage/")
                    }
                    .setNegativeButton(R.string.prusa_slicer_plugin) { _, _ ->
                        openLink("https://plugins.octoprint.org/plugins/prusaslicerthumbnails/")
                    }
                    .setNeutralButton(R.string.cancel, null)
                    .show()
            },
            onRetry = {
                it.showLoading()
                viewModel.reload()
            }
        )
        binding.recyclerViewFileList.adapter = adapter
        viewModel.picasso.observe(viewLifecycleOwner) {
            adapter.picasso = it
        }

        val showLoaderJob = lifecycleScope.launchWhenCreated {
            delay(LOADER_DELAY)
            adapter.showLoading()
        }

        // Load files
        viewModel.loadFiles(navArgs.folder).observe(viewLifecycleOwner) {
            Timber.i(it.toString())
            binding.swipeRefreshLayout.isRefreshing = false
            showLoaderJob.cancel()
            if (it.error) {
                adapter.showError()
            } else {
                adapter.showFiles(
                    folderName = navArgs.folder?.name,
                    files = it.files,
                    showThumbnailHint = it.showThumbnailHint
                )
            }
        }

        // Setup swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.reload()
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
        binding.recyclerViewFileList.setupWithToolbar(requireOctoActivity())
    }

    private fun openLink(url: String) {
        try {
            requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun refreshFiles() {
        Timber.i("Menu closed, triggering refresh")
        viewModel.reload()
        binding.swipeRefreshLayout.isRefreshing = true
    }
}