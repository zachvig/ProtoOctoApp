package de.crysxd.octoapp.base.ui.common.terminal

import android.os.Bundle
import android.text.InputType
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.common.gcodeshortcut.GcodeShortcutLayoutManager
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import kotlinx.android.synthetic.main.fragment_terminal.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.util.*

class TerminalFragment : BaseFragment(R.layout.fragment_terminal) {

    override val viewModel: TerminalViewModel by injectViewModel(Injector.get().viewModelFactory())
    private var initialLayout = true
    private var observeSerialCommunicationsJob: Job? = null
    private var adapter: TerminalAdapter<*>? = null
    private var wasScrolledToBottom = false
    private var oldViewHeight = 0
    private lateinit var shortcutLayoutManager: GcodeShortcutLayoutManager
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
        initialLayout = true

        // Terminal
        initTerminal(
            if (viewModel.isStyledTerminalUsed()) {
                StyledTerminalAdapter()
            } else {
                PlainTerminalAdaper()
            }
        )
        viewModel.terminalFilters.observe(viewLifecycleOwner, {})

        // Shortcuts & Buttons
        viewModel.uiState.observe(viewLifecycleOwner, Observer(this::updateUi))
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

        shortcutLayoutManager = GcodeShortcutLayoutManager(
            layout = buttonList,
            scroller = buttonListScrollView,
            onInsert = ::insertGcode,
            onClicked = { viewModel.executeGcode(it.command) },
            childFragmentManager = childFragmentManager
        )

        // Gcode input
        gcodeInput.setOnActionListener { sendGcodeFromInput() }
        gcodeInput.editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        gcodeInput.editText.maxLines = 100
        gcodeInput.editText.isSingleLine = false
        gcodeInput.editText.imeOptions = EditorInfo.IME_ACTION_UNSPECIFIED
        gcodeInput.editText.setOnKeyListener { _, i, _ ->
            if (i == KeyEvent.KEYCODE_BACK) {
                gcodeInput.editText.clearFocus()
                true
            } else false
        }
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
        observeSerialCommunicationsJob = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
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

    private fun updateUi(uiState: TerminalViewModel.UiState) {
        if (!initialLayout) {
            val transition = AutoTransition()
            transition.excludeTarget(recyclerView, true)
            TransitionManager.beginDelayedTransition(view as ViewGroup, transition)
        }

        shortcutLayoutManager.showGcodes(if (uiState.printing) emptyList() else uiState.gcodes)
        gcodeInput.isVisible = !uiState.printing
        printingHint.isVisible = uiState.printing

        // We are not in initial layout anymore as soon as the gcode arrived
        initialLayout = uiState.gcodes.isEmpty()
    }

    private fun insertGcode(gcode: GcodeHistoryItem) {
        gcodeInput.editText.setText(gcode.command)
    }

    override fun onResume() {
        super.onResume()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        recyclerView.setupWithToolbar(requireOctoActivity())
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(onLayoutListener)
    }

    override fun onPause() {
        super.onPause()
        recyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(onLayoutListener)
    }
}