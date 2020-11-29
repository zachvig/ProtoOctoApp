package de.crysxd.octoapp.pre_print_controls.ui.file_details

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.Injector
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.fragment_file_details.*

class FileDetailsFragment : Fragment(R.layout.fragment_file_details) {

    private val viewModel: FileDetailsViewModel by injectViewModel(Injector.get().viewModelFactory())
    private val adapter by lazy { Adapter() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.file = navArgs<FileDetailsFragmentArgs>().value.file

        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = adapter.itemCount
        viewPager.isUserInputEnabled = false
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = when (adapter.createFragment(position)) {
                is InfoTab -> "Info"
                is GcodeTab -> {
                    val builder = SpannableStringBuilder("Preview")
                    builder.append("    ")
                    val span = ImageSpan(requireContext(), R.drawable.ic_beta)
                    builder.setSpan(span, builder.length - 1, builder.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
                    builder
                }
                else -> ""
            }
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                scrollView.smoothScrollTo(0, if (position == 0) 0 else Int.MAX_VALUE)
                scrollView.isUserInputEnabled = position == 0
            }
        })

        view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom) {
                viewPager.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = view.height - tabs.height
                }
            }
        }
    }

    inner class Adapter : FragmentStateAdapter(this@FileDetailsFragment) {
        private val fragments = listOf(
            InfoTab(),
            GcodeTab()
        )

        override fun getItemCount() = fragments.size

        override fun createFragment(position: Int) = fragments[position]

    }

    override fun onResume() {
        super.onResume()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Prepare
        requireOctoActivity().octo.isVisible = true
        scrollView.setupWithToolbar(requireOctoActivity())
    }
}