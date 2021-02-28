package de.crysxd.octoapp.base.ui.common.help

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import de.crysxd.octoapp.base.databinding.HelpOverviewFragmentBinding
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.feedback.SendFeedbackDialog
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.MenuAdapter
import de.crysxd.octoapp.base.ui.menu.MenuItem
import de.crysxd.octoapp.base.ui.menu.MenuItemStyle
import de.crysxd.octoapp.base.ui.menu.PreparedMenuItem

class HelpOverviewFragment : Fragment() {

    private lateinit var binding: HelpOverviewFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        HelpOverviewFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenCreated {
            binding.introductionView.setOnClickListener {
                Uri.parse("https://www.youtube.com/watch?v=lKJhWnLUrHA").open(it.context)
            }

            binding.contactOptions.adapter = MenuAdapter(
                onClick = {
                    lifecycleScope.launchWhenCreated {
                        it.onClicked(null)
                    }
                },
                onPinItem = {}
            ).also {
                it.menuItems = createContactOptions().prepare()
            }
        }
    }

    private fun createContactOptions() = listOf(
        HelpMenuItem(style = MenuItemStyle.Green, "OctoPrint community") {
            Uri.parse("https://community.octoprint.org/").open(requireContext())
        },
        HelpMenuItem(style = MenuItemStyle.Green, "OctoPrint Discord") {
            Uri.parse("https://discord.com/invite/yA7stPp").open(requireContext())
        },
        HelpMenuItem(style = MenuItemStyle.Green, "I want to report a bug") {
            SendFeedbackDialog().show(childFragmentManager, "bug-report")
        },
        HelpMenuItem(style = MenuItemStyle.Green, "I have an other question") {
            SendFeedbackDialog().show(childFragmentManager, "question")
        },
    )

    private suspend fun List<MenuItem>.prepare() = map {
        PreparedMenuItem(
            title = it.getTitle(requireContext()),
            description = it.getDescription(requireContext()),
            menuItem = it,
            isVisible = it.isVisible(findNavController().currentDestination?.id ?: 0)
        )
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
    }
}