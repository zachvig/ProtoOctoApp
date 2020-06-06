package de.crysxd.octoapp.pre_print_controls.ui.select_file

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_select_file.*

class SelectFileFragment : BaseFragment(R.layout.fragment_select_file) {

    override val viewModel: SelectFileViewModel by injectViewModel()

    private val navArgs by navArgs<SelectFileFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setupWithNavController(findNavController())

        val adapter = SelectFileAdapter() {
            viewModel.selectFile(it)
        }
        recyclerViewFileList.adapter = adapter

        if (navArgs.folder != null) {
            initWithFolder(adapter, navArgs.folder as FileObject.Folder)
        } else {
            initWithRootFolder(adapter)
        }
    }

    private fun initWithFolder(adapter: SelectFileAdapter, folder: FileObject.Folder) {
        adapter.files = folder.children ?: emptyList()
        toolbar.subtitle = folder.display
    }

    private fun initWithRootFolder(adapter: SelectFileAdapter) {
        viewModel.loadRootFiles().observe(this, Observer {
            adapter.files = it
        })
    }
}