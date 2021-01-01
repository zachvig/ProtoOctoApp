package de.crysxd.octoapp.pre_print_controls.ui.file_details

import android.graphics.Rect
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.InsetAwareScreen
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.common.gcode.GcodePreviewFragment
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.Injector
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_file_details.*

class FileDetailsFragment : BaseFragment(R.layout.fragment_file_details), InsetAwareScreen {

    override val viewModel: FileDetailsViewModel by injectViewModel(Injector.get().viewModelFactory())
    private val file by lazy { navArgs<FileDetailsFragmentArgs>().value.file }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.file = file
        viewModel.loading.observe(viewLifecycleOwner) {
            buttonStartPrint.isEnabled = !it
            buttonStartPrint.setText(if (it) R.string.loading else R.string.start_printing)
        }

        buttonStartPrint.setOnClickListener {
            viewModel.startPrint()
        }

        val adapter = Adapter(file)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1
        viewPager.isUserInputEnabled = false
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = when (adapter.createFragment(position)) {
                is InfoTab -> getString(R.string.file_details_tab_info)
                is GcodePreviewFragment -> {
                    val builder = SpannableStringBuilder(getString(R.string.file_details_tab_preview))
                    builder.append("   ")
                    val span = ImageSpan(requireContext(), R.drawable.ic_new)
                    builder.setSpan(span, builder.length - 1, builder.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
                    builder
                }
                else -> ""
            }
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
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
        val position = viewPager.currentItem
        viewPager.updateLayoutParams {
            height = if (position == 0) {
                ViewGroup.LayoutParams.WRAP_CONTENT
            } else {
                requireView().height - tabs.height - requireView().paddingBottom - requireView().paddingTop
            }
        }

        scrollView.isUserInputEnabled = position == 0
        scrollView.isBottomActionAnimationEnabled = false
        scrollView.post {
            scrollView.smoothScrollTo(0, if (position == 0) 0 else Int.MAX_VALUE)
        }
        bottomAction.animate().translationY(if (position == 0) 0f else bottomAction.height.toFloat()).withEndAction {
            scrollView.isBottomActionAnimationEnabled = position == 0
        }.start()

        val toolbarTranslation = if (position == 0) 0f else -requireOctoActivity().octoToolbar.bottom.toFloat()
        requireOctoActivity().octoToolbar.animate().also {
            it.duration = 150
        }.translationY(toolbarTranslation).start()
        requireOctoActivity().octo.animate().also {
            it.duration = 150
        }.translationY(toolbarTranslation).start()
    }

    inner class Adapter(file: FileObject.File) : FragmentStateAdapter(this@FileDetailsFragment) {
        private val fragments = listOf(
            InfoTab(),
            GcodePreviewFragment.createForFile(file, false)
        )

        override fun getItemCount() = fragments.size

        override fun createFragment(position: Int) = fragments[position]

    }

    override fun onResume() {
        super.onResume()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
        requireOctoActivity().octo.isVisible = true
        scrollView.setupWithToolbar(requireOctoActivity(), bottomAction, tabs)
        viewPager.currentItem = 0
    }

    override fun onPause() {
        super.onPause()
        requireOctoActivity().octoToolbar.translationY = 0f
        requireOctoActivity().octo.translationY = 0f
    }

    override fun handleInsets(insets: Rect) {
        view?.updatePadding(top = insets.top, left = insets.left, right = insets.right, bottom = insets.bottom)
        updateLayout()
    }
}