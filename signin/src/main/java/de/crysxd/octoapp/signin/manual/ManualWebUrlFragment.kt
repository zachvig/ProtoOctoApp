package de.crysxd.octoapp.signin.manual

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.signin.databinding.ManualWebUrlFragmentBinding
import de.crysxd.octoapp.signin.di.injectViewModel

class ManualWebUrlFragment : BaseFragment() {
    override val viewModel by injectViewModel<ManualWebUrlViewModel>()
    private lateinit var binding: ManualWebUrlFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ManualWebUrlFragmentBinding.inflate(layoutInflater, container, false).also {
            binding = it
        }.root


}