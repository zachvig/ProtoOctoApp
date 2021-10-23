package de.crysxd.octoapp.filemanager.ui.select_file

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.menu.MenuBottomSheetFragment
import de.crysxd.octoapp.filemanager.R
import de.crysxd.octoapp.filemanager.databinding.SelectFileFragmentBinding
import de.crysxd.octoapp.filemanager.di.injectActivityViewModel
import de.crysxd.octoapp.filemanager.di.injectViewModel
import de.crysxd.octoapp.filemanager.menu.AddItemMenu
import de.crysxd.octoapp.filemanager.menu.FileActionsMenu
import de.crysxd.octoapp.filemanager.menu.SortOptionsMenu
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SelectFileFragment : BaseFragment() {

    private lateinit var binding: SelectFileFragmentBinding
    override val viewModel: SelectFileViewModel by injectViewModel()
    val copyViewModel: MoveAndCopyFilesViewModel by injectActivityViewModel()
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
            onSortOptionsClicked = { MenuBottomSheetFragment.createForMenu(SortOptionsMenu()).show(childFragmentManager) },
            onFileMenuOpened = { MenuBottomSheetFragment.createForMenu(FileActionsMenu(it)).show(childFragmentManager) },
            onHideThumbnailHint = { viewModel.hideThumbnailHint() },
            onRetry = { viewModel.reload() },
            onAddItemClicked = { MenuBottomSheetFragment.createForMenu(AddItemMenu(viewModel.fileOrigin, navArgs.folder)).show(childFragmentManager) },
            onShowThumbnailInfo = {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(getString(R.string.file_manager___thumbnail_info___popup_message))
                    .setPositiveButton(R.string.file_manager___thumbnail_info___popup_cura) { _, _ ->
                        openLink("https://plugins.octoprint.org/plugins/UltimakerFormatPackage/")
                    }
                    .setNegativeButton(R.string.file_manager___thumbnail_info___popup_prusa) { _, _ ->
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

        // Show loading and postpone enter to have a smooth animation if we load very fast
        val delay = 300L
        postponeEnterTransition(delay, TimeUnit.MILLISECONDS)
        val showLoadingJob = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            delay(delay * 2)
            adapter.showLoading()
            startPostponedEnterTransition()
        }

        // Load files
        viewModel.uiState.asLiveData().observe(viewLifecycleOwner) {
            showLoadingJob.cancel()
            startPostponedEnterTransition()
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

        // Observe moving files
        copyViewModel.selectedFile.observe(viewLifecycleOwner) { file ->
            TransitionManager.beginDelayedTransition(binding.root)
            binding.copyControls.isVisible = file != null
            binding.copiedFileName.text = file?.display
            binding.buttonCancelPaste.setOnClickListener { copyViewModel.selectedFile.value = null }
            binding.buttonPasteHere.setOnClickListener {
                file?.let {
                    viewModel.moveFileHere(file, copyFile = copyViewModel.copyFile)
                    copyViewModel.selectedFile.value = null
                }
            }
        }

        // Setup swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.reload()
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