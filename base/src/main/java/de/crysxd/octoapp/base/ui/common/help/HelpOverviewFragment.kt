package de.crysxd.octoapp.base.ui.common.help

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import de.crysxd.octoapp.base.databinding.HelpOverviewFragmentBinding
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity

class HelpOverviewFragment : Fragment() {

    private lateinit var binding: HelpOverviewFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        HelpOverviewFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.introductionView.setOnClickListener {
            Uri.parse("https://www.youtube.com/watch?v=lKJhWnLUrHA").open(it.context)
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
    }
}