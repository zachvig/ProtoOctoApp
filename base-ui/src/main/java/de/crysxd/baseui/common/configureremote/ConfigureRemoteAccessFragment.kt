package de.crysxd.baseui.common.configureremote

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Animatable2
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.*
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.InsetAwareScreen
import de.crysxd.baseui.OctoActivity
import de.crysxd.baseui.R
import de.crysxd.baseui.common.LinkClickMovementMethod
import de.crysxd.baseui.databinding.ConfigureRemoteAccessFragmentBinding
import de.crysxd.baseui.di.injectViewModel
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.utils.CollapsibleToolbarTabsHelper
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ext.toHtml

class ConfigureRemoteAccessFragment : BaseFragment(), InsetAwareScreen {

    override val viewModel by injectViewModel<ConfigureRemoteAccessViewModel>()
    private lateinit var binding: ConfigureRemoteAccessFragmentBinding
    private val adapter by lazy { PagerAdapter(childFragmentManager, lifecycle) }
    private val helper = CollapsibleToolbarTabsHelper()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ConfigureRemoteAccessFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.RemoteConfigScreenOpened)
        super.onViewCreated(view, savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.header.postDelayed({
                (binding.header.drawable as? Animatable2)?.start()
            }, 1000)
        }

        installTabs()

        binding.description.text = getString(R.string.configure_remote_acces___description).toHtml()
        binding.description.movementMethod = LinkClickMovementMethod(LinkClickMovementMethod.OpenWithIntentLinkClickedListener(requireOctoActivity()))

        viewModel.viewEvents.observe(viewLifecycleOwner) {
            if (it.consumed) {
                return@observe
            }
            it.consumed = true

            when (it) {
                is ConfigureRemoteAccessViewModel.ViewEvent.ShowError -> requireOctoActivity().showDialog(
                    message = it.message,
                    neutralButton = getString(R.string.configure_remote_acces___ignore_issue).takeIf { _ -> it.ignoreAction != null }
                        ?: getString(R.string.show_details),
                    neutralAction = { _ -> it.ignoreAction?.invoke() ?: requireOctoActivity().showDialog(it.exception) },
                )

                is ConfigureRemoteAccessViewModel.ViewEvent.Success -> {
                    requireOctoActivity().showSnackbar(
                        OctoActivity.Message.SnackbarMessage(
                            text = { it.getString(R.string.configure_remote_acces___remote_access_configured) },
                            type = OctoActivity.Message.SnackbarMessage.Type.Positive
                        )
                    )
                }

                is ConfigureRemoteAccessViewModel.ViewEvent.OpenUrl ->
                    Uri.parse(it.url).open(requireOctoActivity())
            }
        }
    }

    private fun installTabs() {
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = adapter.getTitle(requireContext(), position)
        }.attach()
        helper.install(
            octoActivity = requireOctoActivity(),
            tabs = binding.tabs,
            tabsContainer = binding.tabsContainer,
            appBar = binding.appBar,
            toolbar = binding.toolbar,
            toolbarContainer = binding.toolbarContainer
        )
    }

    override fun onResume() {
        super.onResume()
        helper.handleResume()
    }

    override fun handleInsets(insets: Rect) {
        helper.handleInsets(insets)
    }

    private class PagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount() = 2

        fun getTitle(context: Context, position: Int) = when (position) {
            0 -> context.getString(R.string.configure_remote_acces___octoeverywhere___title)
            1 -> context.getString(R.string.configure_remote_acces___manual___title)
            else -> throw IllegalStateException("Unknown index $position")
        }

        override fun createFragment(position: Int) = when (position) {
            0 -> ConfigureRemoteAccessOctoEverywhereFragment()
            1 -> ConfigureRemoteAccessManualFragment()
            else -> throw IllegalStateException("Unknown index $position")
        }
    }
}