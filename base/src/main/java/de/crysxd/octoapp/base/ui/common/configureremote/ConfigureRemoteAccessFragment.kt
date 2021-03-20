package de.crysxd.octoapp.base.ui.common.configureremote

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.*
import androidx.transition.TransitionManager
import com.google.android.material.tabs.TabLayout
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.ConfigureRemoteAccessFragmentBinding
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.base.InsetAwareScreen
import de.crysxd.octoapp.base.ui.base.OctoActivity
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity

class ConfigureRemoteAccessFragment : BaseFragment(), InsetAwareScreen {

    override val viewModel by injectViewModel<ConfigureRemoteAccessViewModel>()
    private lateinit var binding: ConfigureRemoteAccessFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ConfigureRemoteAccessFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.tabsContent.getChildAt(tab.position).isVisible = true
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                TransitionManager.beginDelayedTransition(binding.tabsContent)
                binding.tabsContent.getChildAt(tab.position).isVisible = false
            }

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })


        binding.tabsContent.doOnLayout {
            val tabContentHeight = binding.tabsContent.children.map {
                it.measure(
                    View.MeasureSpec.makeMeasureSpec(binding.tabsContent.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                )
                it.measuredHeight
            }.maxOrNull() ?: 0

            binding.tabsContent.updateLayoutParams {
                height = tabContentHeight
            }
        }

        binding.saveUrl.setOnClickListener {
            viewModel.setRemoteUrl(binding.webUrlInput.editText.text.toString(), false)
        }

        viewModel.viewState.observe(viewLifecycleOwner) {
            binding.saveUrl.isEnabled = it !is ConfigureRemoteAccessViewModel.ViewState.Loading
            binding.saveUrl.setText(if (binding.saveUrl.isEnabled) R.string.configure_remote_acces___manual___button else R.string.loading)
            (it as? ConfigureRemoteAccessViewModel.ViewState.Updated)?.let {
                binding.webUrlInput.editText.setText(it.remoteWebUrl)
            }
        }

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

                ConfigureRemoteAccessViewModel.ViewEvent.Success -> {
                    binding.webUrlInput.editText.clearFocus()
                    requireOctoActivity().showSnackbar(
                        OctoActivity.Message.SnackbarMessage(
                            text = { it.getString(R.string.configure_remote_acces___remote_access_configured) },
                            type = OctoActivity.Message.SnackbarMessage.Type.Positive
                        )
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
    }

    override fun handleInsets(insets: Rect) {
        val lastBottom = binding.root.paddingBottom
        binding.root.updatePadding(
            left = insets.left,
            top = insets.top,
            right = insets.right,
            bottom = insets.bottom
        )

        if (lastBottom != 0 && insets.bottom > lastBottom) {
            // Keyboard opened
            binding.root.smoothScrollTo(0, Int.MAX_VALUE)
        }
    }
}