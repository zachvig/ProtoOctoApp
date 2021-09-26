package de.crysxd.octoapp.help.faq

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
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.common.feedback.SendFeedbackDialog
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.menu.Menu
import de.crysxd.baseui.menu.MenuAdapter
import de.crysxd.baseui.menu.MenuHost
import de.crysxd.baseui.menu.MenuItem
import de.crysxd.baseui.menu.MenuItemClickExecutor
import de.crysxd.baseui.menu.MenuItemStyle
import de.crysxd.baseui.menu.PreparedMenuItem
import de.crysxd.baseui.menu.main.ShowNewsMenuItem
import de.crysxd.baseui.menu.main.ShowTutorialsMenuItem
import de.crysxd.baseui.menu.main.ShowYoutubeMenuItem
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ext.suspendedAwait
import de.crysxd.octoapp.help.R
import de.crysxd.octoapp.help.databinding.HelpFragmentBinding
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.concurrent.TimeUnit

class HelpFragment : Fragment(), MenuHost {

    private lateinit var binding: HelpFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        HelpFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenCreated {
            binding.introductionView.setOnClickListener {
                Uri.parse(Firebase.remoteConfig.getString("introduction_video_url")).open(requireOctoActivity())
            }

            binding.contactOptions.adapter = MenuAdapter(
                onClick = ::handleItemClick,
            ).also {
                it.menuItems = createContactOptions().prepare()
            }

            startPostponedEnterTransition()

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

            val tutorial = listOf(
                ShowTutorialsMenuItem(showAsHalfWidth = false),
                ShowNewsMenuItem(),
                ShowYoutubeMenuItem()
            ).prepare()

            val faq = try {
                createFaqItems().prepare()
            } catch (e: Exception) {
                Timber.e(e)
                emptyList()
            }

            val bugs = try {
                createBugList().prepare()
            } catch (e: Exception) {
                Timber.e(e)
                emptyList()
            }

            if (delayed) {
                TransitionManager.beginDelayedTransition(binding.root)
            }

            // Show Tutorial
            binding.tutorial.adapter = MenuAdapter(
                onClick = ::handleTutorialClick,
            ).also {
                it.menuItems = tutorial
            }

            // Show FAQ
            binding.progressBar.isVisible = false
            binding.faqError.isVisible = faq.isEmpty()
            binding.faqOptions.adapter = MenuAdapter(
                onClick = ::handleItemClick,
            ).also {
                it.menuItems = faq
            }

            // Show bugs
            binding.bugsTitle.isVisible = bugs.isNotEmpty()
            binding.bugsList.isVisible = bugs.isNotEmpty()
            binding.bugsList.adapter = MenuAdapter(
                onClick = ::handleItemClick,
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

    private fun handleTutorialClick(item: MenuItem) {
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            item.onClicked(MenuItemClickExecutor(this@HelpFragment, MenuAdapter({})))
        }
    }

    private fun createContactOptions(): List<HelpMenuItem> {
        val listOf = listOf(
            HelpMenuItem(style = MenuItemStyle.Green, getString(R.string.help___octoprint_community)) {
                Uri.parse("https://community.octoprint.org/").open(requireOctoActivity())
            },
            HelpMenuItem(style = MenuItemStyle.Green, getString(R.string.help___octoprint_discord)) {
                Uri.parse("https://discord.octoprint.org/").open(requireOctoActivity())
            },
            HelpMenuItem(style = MenuItemStyle.Green, getString(R.string.help___report_a_bug)) {
                SendFeedbackDialog.create(isForBugReport = true).show(childFragmentManager, "bug-report")
            },
            HelpMenuItem(style = MenuItemStyle.Green, getString(R.string.help___ask_a_question)) {
                SendFeedbackDialog().show(childFragmentManager, "question")
            },
        )
        return listOf
    }

    private fun createFaqItems() = Firebase.remoteConfig.getString("faq").let {
        parseFaqsFromJson(it)
    }.filter {
        !it.title.isNullOrBlank() && !it.content.isNullOrBlank() && it.hidden != true
    }.map {
        HelpMenuItem(style = MenuItemStyle.Yellow, title = it.title ?: "") {
            findNavController().navigate(HelpFragmentDirections.actionShowFaq(faqId = it.id, bug = null))
        }
    }

    private fun createBugList() = Firebase.remoteConfig.getString("known_bugs").let {
        parseKnownBugsFromJson(it)
    }.filter {
        !it.title.isNullOrBlank() && !it.content.isNullOrBlank()
    }.map {
        HelpMenuItem(style = MenuItemStyle.Red, title = it.title ?: "") {
            findNavController().navigate(HelpFragmentDirections.actionShowFaq(faqId = null, bug = it))
        }
    }

    private suspend fun List<MenuItem>.prepare() = map {
        PreparedMenuItem(
            title = it.getTitle(requireContext()),
            right = it.getRightDetail(requireContext()),
            description = it.getDescription(requireContext()),
            menuItem = it,
            isVisible = it.isVisible(findNavController().currentDestination?.id ?: 0),
            badgeCount = it.getBadgeCount()
        )
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        binding.scrollView.setupWithToolbar(requireOctoActivity())
    }

    override fun pushMenu(subMenu: Menu) = Unit

    override fun closeMenu() = Unit

    override fun getNavController() = findNavController()

    override fun getMenuActivity() = requireActivity()

    override fun getMenuFragmentManager() = childFragmentManager

    override fun getHostFragment() = this

    override fun reloadMenu() = Unit

    override fun isCheckBoxChecked() = false

    override fun suppressSuccessAnimationForNextAction() = Unit

    override fun consumeSuccessAnimationForNextActionSuppressed() = false
}