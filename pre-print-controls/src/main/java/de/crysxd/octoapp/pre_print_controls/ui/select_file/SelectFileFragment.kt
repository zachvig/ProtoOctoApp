package de.crysxd.octoapp.pre_print_controls.ui.select_file

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import timber.log.Timber

class SelectFileFragment : BaseFragment(R.layout.fragment_select_file) {

    override val viewModel: SelectFileViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.files.observe(this, Observer {
            Timber.i("Loaded ${it.size} files")
        })
    }

}