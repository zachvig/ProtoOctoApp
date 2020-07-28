package de.crysxd.octoapp.pre_print_controls.ui.select_file

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_select_file.*

class SelectFileFragment : BaseFragment(R.layout.fragment_select_file) {

    override val viewModel: SelectFileViewModel by injectViewModel()

    private val navArgs by navArgs<SelectFileFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = SelectFileAdapter {
            viewModel.selectFile(it)
        }
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
        recyclerViewFileList.setupWithToolbar(requireOctoActivity())
    }

    private fun initWithFolder(adapter: SelectFileAdapter, folder: FileObject.Folder) {
        adapter.setFiles(folder.children ?: emptyList(), navArgs.showThumbnailHint)
        adapter.title = folder.name
    }

    private fun initWithRootFolder(adapter: SelectFileAdapter) {
        progressIndicator.isVisible = true
        viewModel.loadRootFiles().observe(viewLifecycleOwner, Observer {
            progressIndicator.isVisible = false
            adapter.setFiles(it.files, it.showThumbnailHint)
            adapter.title = "Select a file to print"
        })
    }
}