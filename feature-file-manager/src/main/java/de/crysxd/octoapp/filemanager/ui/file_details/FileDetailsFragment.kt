package de.crysxd.octoapp.filemanager.ui.file_details

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.InsetAwareScreen
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.common.gcode.GcodePreviewFragment
import de.crysxd.baseui.ext.optionallyRequestOctoActivity
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.menu.MenuBottomSheetFragment
import de.crysxd.baseui.menu.material.MaterialPluginMenu
import de.crysxd.octoapp.filemanager.R
import de.crysxd.octoapp.filemanager.databinding.FileDetailsFragmentBinding
import de.crysxd.octoapp.filemanager.di.injectViewModel
import de.crysxd.octoapp.octoprint.models.files.FileObject
import java.util.concurrent.TimeUnit

class FileDetailsFragment : BaseFragment(), InsetAwareScreen {

    override val viewModel: FileDetailsViewModel by injectViewModel()
    private val args by navArgs<FileDetailsFragmentArgs>()
    private val originalOctoTranslationY by lazy { requireOctoActivity().octo.translationY }
    private lateinit var binding: FileDetailsFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postponeEnterTransition(1000, TimeUnit.MILLISECONDS)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FileDetailsFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.file = args.file
        viewModel.loading.observe(viewLifecycleOwner) {
            binding.buttonStartPrint.isEnabled = !it
            binding.buttonStartPrint.setText(if (it) R.string.loading else R.string.start_printing)
        }
        viewModel.viewEvents.observe(viewLifecycleOwner) {
            if (!it.isConsumed) {
                it.isConsumed = true
                when (it) {
                    is FileDetailsViewModel.ViewEvent.MaterialSelectionRequired ->
                        MenuBottomSheetFragment.createForMenu(MaterialPluginMenu(startPrintAfterSelection = args.file))
                            .show(childFragmentManager)

                    is FileDetailsViewModel.ViewEvent.PrintStarted -> {
                        // We started a print. Soon OctoPrint will report the print as started. We want to automatically
                        // navigate to the print state, but by default we don't allow auto navigation here so we need to
                        // make an exception.
                        requireOctoActivity().enforceAllowAutomaticNavigationFromCurrentDestination()
                    }
                }
            }
        }

        binding.buttonStartPrint.setOnClickListener {
            viewModel.startPrint()
        }

        viewModel.canStartPrint.observe(viewLifecycleOwner) {
            binding.buttonStartPrint.isEnabled = it
        }

        val adapter = Adapter(args.file)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.isUserInputEnabled = false
        TabLayoutMediator(binding.tabs, binding.viewPager) { tab, position ->
            tab.text = when (adapter.createFragment(position)) {
                is InfoTabFragment -> getString(R.string.file_details_tab_info)
                is GcodePreviewFragment -> getString(R.string.file_details_tab_preview)
                else -> ""
            }
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateLayout()
            }
        })

        view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom) {
                updateLayout()
            }
        }
    }

    private fun updateLayout() {
        startPostponedEnterTransition()
        binding.viewPager.post {
            val activity = optionallyRequestOctoActivity() ?: return@post
            val position = binding.viewPager.currentItem
            binding.viewPager.updateLayoutParams {
                height = if (position == 0) {
                    ViewGroup.LayoutParams.WRAP_CONTENT
                } else {
                    requireView().height - binding.tabs.height - requireView().paddingBottom - requireView().paddingTop - binding.container.paddingTop
                }
            }

            binding.scrollView.isUserInputEnabled = position == 0
            binding.scrollView.isBottomActionAnimationEnabled = false
            binding.scrollView.post {
                binding.scrollView.smoothScrollTo(0, if (position == 0) 0 else Int.MAX_VALUE)
            }
            binding.bottomAction.animate().translationY(if (position == 0) 0f else binding.bottomAction.height.toFloat()).withEndAction {
                binding.scrollView.isBottomActionAnimationEnabled = position == 0
            }.start()

            val toolbarTranslation = if (position == 0) 0f else -activity.octoToolbar.bottom.toFloat()
            activity.octoToolbar.animate().also {
                it.duration = 150
            }.translationY(toolbarTranslation).start()
            activity.octo.animate().also {
                it.duration = 150
            }.translationY(toolbarTranslation + originalOctoTranslationY).start()
        }
    }

    inner class Adapter(file: FileObject.File) : FragmentStateAdapter(this@FileDetailsFragment) {
        private val fragments = listOf(
            InfoTabFragment(),
            GcodePreviewFragment.createForFile(file, false)
        )

        override fun getItemCount() = fragments.size

        override fun createFragment(position: Int) = fragments[position]

    }

    override fun onResume() {
        super.onResume()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
        requireOctoActivity().octo.isVisible = true
        binding.scrollView.setupWithToolbar(requireOctoActivity(), binding.bottomAction, binding.tabsContainer)
        binding.viewPager.currentItem = 0

        // Save translation
        originalOctoTranslationY
    }

    override fun onPause() {
        super.onPause()
        requireOctoActivity().octoToolbar.translationY = 0f
        requireOctoActivity().octo.translationY = originalOctoTranslationY
    }

    override fun handleInsets(insets: Rect) {
        view?.updatePadding(top = insets.top, left = insets.left, right = insets.right, bottom = insets.bottom)
        updateLayout()
    }
}