package de.crysxd.octoapp.base.ui.common.configureremote

import android.graphics.Rect
import android.graphics.drawable.Animatable2
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.transition.TransitionManager
import com.google.android.material.tabs.TabLayout
import de.crysxd.octoapp.base.OctoAnalytics
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.ConfigureRemoteAccessFragmentBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ext.toHtml
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.base.InsetAwareScreen
import de.crysxd.octoapp.base.ui.base.OctoActivity
import de.crysxd.octoapp.base.ui.common.LinkClickMovementMethod
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import timber.log.Timber

class ConfigureRemoteAccessFragment : BaseFragment(), InsetAwareScreen {

    override val viewModel by injectViewModel<ConfigureRemoteAccessViewModel>()
    private lateinit var binding: ConfigureRemoteAccessFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ConfigureRemoteAccessFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        OctoAnalytics.logEvent(OctoAnalytics.Event.RemoteConfigScreenOpened)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.header.postDelayed({
                (binding.header.drawable as? Animatable2)?.start()
            }, 1000)
        }

        binding.description.text = getString(R.string.configure_remote_acces___description).toHtml()
        binding.description.movementMethod = LinkClickMovementMethod(LinkClickMovementMethod.OpenWithIntentLinkClickedListener(requireOctoActivity()))
        binding.saveUrl.setOnClickListener {
            viewModel.setRemoteUrl(binding.webUrlInput.editText.text.toString(), false)
        }

        binding.connectOctoEverywhere.setOnClickListener {
            viewModel.getOctoEverywhereAppPortalUrl()
        }

        binding.webUrlInput.backgroundTint = ContextCompat.getColor(requireContext(), R.color.input_background_alternative)

        viewModel.viewState.observe(viewLifecycleOwner) {
            TransitionManager.beginDelayedTransition(binding.root)
            binding.saveUrl.isEnabled = it !is ConfigureRemoteAccessViewModel.ViewState.Loading
            binding.saveUrl.setText(if (binding.saveUrl.isEnabled) R.string.configure_remote_acces___manual___button else R.string.loading)
            (it as? ConfigureRemoteAccessViewModel.ViewState.Updated)?.let { _ ->
                binding.webUrlInput.editText.setText(it.remoteWebUrl)
                binding.octoEverywhereConnected.isVisible = it.remoteWebUrl == it.octoEverywhereConnection?.fullUrl
            }

            measureTabContents()
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

                is ConfigureRemoteAccessViewModel.ViewEvent.Success -> {
                    binding.webUrlInput.editText.clearFocus()
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

    private fun measureTabContents() {
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

    override fun onStart() {
        super.onStart()
        requireOctoActivity().octo.isVisible = false
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        requireView().doOnLayout {
            measureTabContents()
        }
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