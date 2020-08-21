package de.crysxd.octoapp.base.ui.common.terminal

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import kotlinx.android.synthetic.main.fragment_terminal.*
import kotlinx.coroutines.flow.collect

class TerminalFragment : Fragment(R.layout.fragment_terminal) {

    private val viewModel: TerminalViewModel by injectViewModel(Injector.get().viewModelFactory())
    private var initialLayout = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Terminal
        val adapter = PlainTerminalAdaper()
        recyclerView.adapter = adapter
        lifecycleScope.launchWhenCreated {
            val (old, flow) = viewModel.observeSerialCommunication()

            // Add all items we have so far and scroll to bottom
            adapter.initWithItems(old)
            recyclerView.scrollToPosition(adapter.itemCount - 1)

            // Append all new items
            flow.collect {
                adapter.appendItem(it)

                // If we are scrolled to the end, scroll down again after we added the item
                if (!recyclerView.canScrollVertically(1)) {
                    recyclerView.scrollToPosition(adapter.itemCount - 1)
                }
            }
        }

        // Shortcuts & Buttons
        viewModel.gcodes.observe(viewLifecycleOwner, Observer(this::showGcodeShortcuts))
        buttonClear.setOnClickListener {
            adapter.clear()
            viewModel.clear()
        }
    }

    private fun showGcodeShortcuts(gcodes: List<GcodeHistoryItem>) {
        if (!initialLayout) {
            TransitionManager.beginDelayedTransition(buttonList)
        }

        // Remove all old views except the predefined buttons (those have ids)
        val removedViews = mutableListOf<Button>()
        buttonList.children.toList().forEach {
            if (it.id == 0) {
                buttonList.removeView(it)
                removedViews.add(it as Button)
            }
        }

        // Add new views
        gcodes.forEach { gcode ->
            val button = removedViews.firstOrNull { it.text.toString() == gcode.command }
                ?: LayoutInflater.from(requireContext()).inflate(R.layout.widget_gcode_button, buttonList, false) as Button
            button.text = gcode.command
            button.layoutParams = (button.layoutParams as LinearLayout.LayoutParams).also {
                it.marginStart = requireContext().resources.getDimensionPixelSize(R.dimen.margin_1)
            }
            buttonList.addView(button, 0)
            button.setOnClickListener {
                viewModel.executeGcode(button.text.toString())
            }
        }

        // Scroll to end of list the first time we populate the buttons
        if (initialLayout) {
            initialLayout = false
            buttonListScrollView.doOnNextLayout {
                buttonListScrollView.scrollTo(buttonList.width, 0)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        recyclerView.setupWithToolbar(requireOctoActivity())
    }
}