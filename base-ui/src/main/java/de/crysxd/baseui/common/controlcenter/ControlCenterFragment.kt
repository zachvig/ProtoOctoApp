package de.crysxd.baseui.common.controlcenter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.databinding.ControlCenterFragmentBinding
import de.crysxd.baseui.di.injectViewModel
import de.crysxd.baseui.ext.requireOctoActivity
import timber.log.Timber

class ControlCenterFragment : BaseFragment() {
    override val viewModel by injectViewModel<ControlCenterViewModel>()
    private lateinit var binding: ControlCenterFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ControlCenterFragmentBinding.inflate(inflater, container, false).also {
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("CREATE")
        requireOctoActivity().applyInsetsToView(binding.root)

        binding.imageButton.setOnClickListener {
            Toast.makeText(requireContext(), "Hello world!", Toast.LENGTH_SHORT).show()
        }

        val adapter = ControlCenterAdapter()
        binding.list.adapter = adapter
        viewModel.viewState.observe(viewLifecycleOwner) {
            adapter.instances = it
        }
    }

    override fun onStart() {
        super.onStart()
        Timber.i("START")
    }

    override fun onStop() {
        super.onStop()
        Timber.i("STOP")
    }
}