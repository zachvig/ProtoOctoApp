package de.crysxd.octoapp.pre_print_controls.ui.select_file

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_select_file.*
import timber.log.Timber

class SelectFileFragment : BaseFragment(R.layout.fragment_select_file) {

    override val viewModel: SelectFileViewModel by injectViewModel()

    private val navArgs by navArgs<SelectFileFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SelectFileAdapter(
            onFileSelected = {
                viewModel.selectFile(it)
            },
            onHideThumbnailHint = {
                viewModel.hideThumbnailHint()
            },
            onShowThumbnailInfo = {
                AlertDialog.Builder(requireContext())
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
        recyclerViewFileList.adapter = adapter

        if (navArgs.folder != null) {
            initWithFolder(adapter, navArgs.folder as FileObject.Folder)
        } else {
            initWithRootFolder(adapter)
        }

        viewModel.picasso.observe(viewLifecycleOwner, Observer(adapter::updatePicasso))
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
        fileListScroller.setupWithToolbar(requireOctoActivity())
    }

    private fun initWithFolder(adapter: SelectFileAdapter, folder: FileObject.Folder) {
        adapter.showFiles(
            folderName = folder.name,
            files = folder.children ?: emptyList(),
            showThumbnailHint = navArgs.showThumbnailHint
        )
    }

    private fun initWithRootFolder(adapter: SelectFileAdapter) {
        adapter.showLoading()
        viewModel.loadRootFiles().observe(viewLifecycleOwner, Observer {
            if (it.error) {
                adapter.showError()
            } else {
                adapter.showFiles(
                    folderName = null,
                    files = it.files,
                    showThumbnailHint = it.showThumbnailHint
                )
            }
        })
    }

    private fun openLink(url: String) {
        try {
            requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}