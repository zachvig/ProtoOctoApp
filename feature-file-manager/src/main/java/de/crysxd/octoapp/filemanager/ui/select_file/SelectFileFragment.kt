package de.crysxd.octoapp.filemanager.ui.select_file

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.filemanager.R
import de.crysxd.octoapp.filemanager.databinding.SelectFileFragmentBinding
import de.crysxd.octoapp.filemanager.di.injectViewModel
import de.crysxd.octoapp.filemanager.menu.AddItemMenu
import de.crysxd.octoapp.filemanager.menu.FileActionsMenu
import de.crysxd.octoapp.octoprint.models.files.FileObject
import timber.log.Timber

// Delay the initial loading display a little. Usually we are on fast local networks so the
// loader would just flash up for a split second which doesn't look nice
const val LOADER_DELAY = 400L

class SelectFileFragment : BaseFragment() {

    private lateinit var binding: SelectFileFragmentBinding
    override val viewModel: SelectFileViewModel by injectViewModel()
    private val navArgs by navArgs<SelectFileFragmentArgs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        SelectFileFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup adapter
        val adapter = SelectFileAdapter(
            context = requireContext(),
            onFileSelected = {
                if (it is FileObject.Folder || it.isPrintable) {
                    viewModel.selectFile(it)
                } else {
                    MenuBottomSheetFragment.createForMenu(FileActionsMenu(it)).show(childFragmentManager)
                }
            },
            onFileMenuOpened = { MenuBottomSheetFragment.createForMenu(FileActionsMenu(it)).show(childFragmentManager) },
            onHideThumbnailHint = { viewModel.hideThumbnailHint() },
            onRetry = { viewModel.loadFiles(navArgs.folder, reload = true) },
            onAddItemClicked = { MenuBottomSheetFragment.createForMenu(AddItemMenu(viewModel.fileOrigin, navArgs.folder)).show(childFragmentManager) },
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
        )

        binding.recyclerViewFileList.adapter = adapter
        viewModel.picasso.observe(viewLifecycleOwner) {
            adapter.picasso = it
        }

        // Load files
        adapter.showLoading()
        viewModel.uiState.asLiveData().observe(viewLifecycleOwner) {
            Timber.i("UiState: $it")
            when (it) {
                is SelectFileViewModel.UiState.DataReady -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    adapter.showFiles(
                        files = it.files,
                        showThumbnailHint = it.showThumbnailHint,
                        folderName = navArgs.folder?.name
                    )
                }
                is SelectFileViewModel.UiState.Error -> {
                    binding.swipeRefreshLayout.isRefreshing = false
                    adapter.showError()
                }
                is SelectFileViewModel.UiState.Loading -> adapter.showLoading()
            }
        }

        // Setup swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadFiles(navArgs.folder, reload = true)
        }
    }

    override fun onStart() {
        super.onStart()

        viewModel.setupThumbnailHint(navArgs.showThumbnailHint)
        viewModel.loadFiles(navArgs.folder)

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
}