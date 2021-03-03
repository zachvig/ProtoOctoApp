package de.crysxd.octoapp.base.ui.common.help

import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.crysxd.octoapp.base.databinding.HelpFragmentBinding
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ext.suspendedAwait
import de.crysxd.octoapp.base.feedback.SendFeedbackDialog
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.*
import de.crysxd.octoapp.base.ui.menu.main.PrivacyMenu
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.TimeUnit

class HelpFragment : Fragment() {

    private lateinit var binding: HelpFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        HelpFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenCreated {
            binding.introductionView.setOnClickListener {
                Uri.parse(Firebase.remoteConfig.getString("introduction_video_url")).open(it.context)
            }

            binding.dataPrivacy.setOnClickListener {
                MenuBottomSheetFragment.createForMenu(PrivacyMenu()).show(childFragmentManager)
            }

            binding.contactOptions.adapter = MenuAdapter(
                onClick = ::handleItemClick,
                onPinItem = {}
            ).also {
                it.menuItems = createContactOptions().prepare()
            }

            // Load FAQ (if remote config is old)
            val fetchAgeMinutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - Firebase.remoteConfig.info.fetchTimeMillis)
            val delayed = if (fetchAgeMinutes > 30) {
                try {
                    Firebase.remoteConfig.fetchAndActivate().suspendedAwait()
                    delay(500)
                } catch (e: Exception) {
                    Timber.e(e)
                }
                true
            } else {
                false
            }

            val faq = createFaqItems().prepare()
            val bugs = createBugList().prepare()

            if (delayed) {
                TransitionManager.beginDelayedTransition(binding.root)
            }

            // Show FAQ
            binding.progressBar.isVisible = false
            binding.faqError.isVisible = faq.isEmpty()
            binding.faqOptions.adapter = MenuAdapter(
                onClick = ::handleItemClick,
                onPinItem = {}
            ).also {
                it.menuItems = faq
            }

            // Show bugs
            binding.bugsTitle.isVisible = bugs.isNotEmpty()
            binding.bugsList.isVisible = bugs.isNotEmpty()
            binding.bugsList.adapter = MenuAdapter(
                onClick = ::handleItemClick,
                onPinItem = {}
            ).also {
                it.menuItems = bugs
            }
        }
    }

    private fun handleItemClick(item: MenuItem) {
        lifecycleScope.launchWhenCreated {
            item.onClicked(null)
        }
    }

    private fun createContactOptions() = listOf(
        HelpMenuItem(style = MenuItemStyle.Green, "OctoPrint community") {
            Uri.parse("https://community.octoprint.org/").open(requireContext())
        },
        HelpMenuItem(style = MenuItemStyle.Green, "OctoPrint Discord") {
            Uri.parse("https://discord.octoprint.org/").open(requireContext())
        },
        HelpMenuItem(style = MenuItemStyle.Green, "I want to report a bug") {
            SendFeedbackDialog().show(childFragmentManager, "bug-report")
        },
        HelpMenuItem(style = MenuItemStyle.Green, "I have an other question") {
            SendFeedbackDialog().show(childFragmentManager, "question")
        },
    )

    private fun createFaqItems() = Firebase.remoteConfig.getString("faq").let {
        Gson().fromJson<List<Faq>>(it, object : TypeToken<ArrayList<Faq>>() {}.type)
    }.filter {
        !it.title.isNullOrBlank() && !it.content.isNullOrBlank()
    }.map {
        HelpMenuItem(style = MenuItemStyle.Yellow, title = it.title ?: "") {
            findNavController().navigate(HelpFragmentDirections.actionShowFaq(faq = it, bug = null))
        }
    }

    private fun createBugList() = Firebase.remoteConfig.getString("known_bugs").let {
        Gson().fromJson<List<KnownBug>>(it, object : TypeToken<ArrayList<KnownBug>>() {}.type)
    }.filter {
        !it.title.isNullOrBlank() && !it.content.isNullOrBlank()
    }.map {
        HelpMenuItem(style = MenuItemStyle.Red, title = it.title ?: "") {
            findNavController().navigate(HelpFragmentDirections.actionShowFaq(faq = null, bug = it))
        }
    }

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
        binding.scrollView.setupWithToolbar(requireOctoActivity())
    }
}