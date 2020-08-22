package de.crysxd.octoapp.base.ui.common.terminal

import android.os.Bundle
import android.text.InputType
import android.transition.TransitionManager
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.util.*

class TerminalFragment : Fragment(R.layout.fragment_terminal) {

    private val viewModel: TerminalViewModel by injectViewModel(Injector.get().viewModelFactory())
    private var initialLayout = true
    private var observeSerialCommunicationsJob: Job? = null
    private var adapter: TerminalAdapter<*>? = null
    private var wasScrolledToBottom = false
    private var oldViewHeight = 0
    private val onLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        // If we are scrolled to the bottom right now, make sure to restore the position
        // after the keyboard is shown
        if (oldViewHeight != recyclerView.height) {
            val force = wasScrolledToBottom
            this.view?.doOnNextLayout {
                scrollToBottom(force)
            }
        }

        // If keyboard hidden
        if (oldViewHeight < recyclerView.height) {
            gcodeInput.editText.clearFocus()
            Timber.i("Keyboard hidden")
        }
        oldViewHeight = recyclerView.height
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Terminal
        initTerminal(
            if (viewModel.isStyledTerminalUsed()) {
                StyledTerminalAdapter()
            } else {
                PlainTerminalAdaper()
            }
        )
        viewModel.terminalFilters.observe(viewLifecycleOwner, Observer {})

        // Shortcuts & Buttons
        viewModel.gcodes.observe(viewLifecycleOwner, Observer(this::showGcodeShortcuts))
        buttonClear.setOnClickListener {
            adapter?.clear()
            viewModel.clear()
        }
        buttonFilters.text = viewModel.selectedTerminalFilters.size.toString()
        buttonFilters.setOnClickListener {
            val availableFilters = (viewModel.terminalFilters.value ?: emptyList()).map {
                Pair(it, viewModel.selectedTerminalFilters.contains(it))
            }

            TerminalFilterDialogFactory().showDialog(
                requireContext(),
                availableFilters
            ) {
                viewModel.selectedTerminalFilters = it.filter { it.second }.map { it.first }
                buttonFilters.text = viewModel.selectedTerminalFilters.size.toString()
                initTerminal(adapter ?: StyledTerminalAdapter())
            }
        }
        buttonToggleStyled.setOnClickListener {
            if (adapter is StyledTerminalAdapter) {
                initTerminal(PlainTerminalAdaper())
                viewModel.setStyledTerminalUsed(false)
            } else {
                initTerminal(StyledTerminalAdapter())
                viewModel.setStyledTerminalUsed(true)
            }
        }

        // Gcode input
        gcodeInput.setOnActionListener { sendGcodeFromInput() }
        gcodeInput.editText.setOnEditorActionListener { _, _, _ -> sendGcodeFromInput(); true }
        gcodeInput.editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        gcodeInput.editText.imeOptions = EditorInfo.IME_ACTION_SEND
    }

    private fun sendGcodeFromInput() {
        val input = gcodeInput.editText.text.toString().toUpperCase(Locale.ENGLISH)
        if (input.isNotBlank()) {
            viewModel.executeGcode(input)
            gcodeInput.editText.text = null
        }
    }

    private fun initTerminal(adapter: TerminalAdapter<*>) {
        recyclerView.adapter = adapter
        observeSerialCommunicationsJob?.cancel()
        observeSerialCommunicationsJob = lifecycleScope.launchWhenCreated {
            val (old, flow) = viewModel.observeSerialCommunication()

            // Add all items we have so far and scroll to bottom
            adapter.initWithItems(old)
            recyclerView.scrollToPosition(adapter.itemCount - 1)

            // Append all new items
            flow.collect {
                adapter.appendItem(it)
                scrollToBottom()
            }
        }


        buttonToggleStyled.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (adapter is StyledTerminalAdapter) {
                R.drawable.ic_round_code_24
            } else {
                R.drawable.ic_round_brush_24
            }, 0, 0, 0
        )

        this.adapter = adapter
    }

    private fun scrollToBottom(forced: Boolean = false) {
        // If we are scrolled to the end, scroll down again after we added the item
        adapter?.let {
            if (forced || !recyclerView.canScrollVertically(1)) {
                wasScrolledToBottom = true
                recyclerView.scrollToPosition(it.getItemCount() - 1)
            }
        }
    }

    private fun showGcodeShortcuts(gcodes: List<GcodeHistoryItem>) {
        if (!initialLayout) {
            TransitionManager.beginDelayedTransition(buttonList)
        } else {
            buttonList.children.forEach { it.tag = true }
        }

        // Remove all old views except the predefined buttons (those have tag == true)
        val removedViews = mutableListOf<Button>()
        buttonList.children.toList().forEach {
            if (it.tag != true) {
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
            button.setOnLongClickListener {
                gcodeInput.editText.setText(gcode.command)
                true
            }
        }

        // Scroll to end of list the first time we populate the buttons
        if (initialLayout) {
            initialLayout = false
            buttonListScrollView.doOnNextLayout {
                buttonListScrollView.scrollTo(buttonList.width, 0)
            }
        }
        gcodeInput.editText.setOnKeyListener { _, i, _ ->
            if (i == KeyEvent.KEYCODE_BACK) {
                gcodeInput.editText.clearFocus()
                true
            } else false
        }
    }

    override fun onStart() {
        super.onStart()
        recyclerView.setupWithToolbar(requireOctoActivity())
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(onLayoutListener)
    }

    override fun onStop() {
        super.onStop()
        recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(onLayoutListener)
    }
}