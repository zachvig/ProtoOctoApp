package de.crysxd.octoapp.pre_print_controls.ui.select_file

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_select_file.*
import kotlinx.coroutines.delay
import timber.log.Timber

// Delay the initial loading display a little. Usually we are on fast local networks so the
// loader would just flash up for a split second which doesn't look nice
const val LOADER_DELAY = 800L

class SelectFileFragment : BaseFragment(R.layout.fragment_select_file) {

    override val viewModel: SelectFileViewModel by injectViewModel()

    private val navArgs by navArgs<SelectFileFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup adapter
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
        viewModel.picasso.observe(viewLifecycleOwner, Observer(adapter::updatePicasso))
        val showLoaderJob = lifecycleScope.launchWhenCreated {
            delay(LOADER_DELAY)
            adapter.showLoading()
        }

        // Load files
        viewModel.loadFiles(navArgs.folder).observe(viewLifecycleOwner, Observer {
            swipeRefreshLayout.isRefreshing = false
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
        })

        // Setup swipe to refresh
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.reload()
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
        recyclerViewFileList.setupWithToolbar(requireOctoActivity())
    }

    private fun openLink(url: String) {
        try {
            requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}