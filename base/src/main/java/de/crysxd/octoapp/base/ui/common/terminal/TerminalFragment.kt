package de.crysxd.octoapp.base.ui.common.terminal

import android.os.Bundle
import android.text.InputType
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.TerminalFragmentBinding
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.ui.base.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.common.gcodeshortcut.GcodeShortcutLayoutManager
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import timber.log.Timber

class TerminalFragment : BaseFragment() {

    private lateinit var binding: TerminalFragmentBinding
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
        if (oldViewHeight != binding.recyclerView.height) {
            val force = wasScrolledToBottom
            this.view?.doOnNextLayout {
                scrollToBottom(force)
            }
        }

        // If keyboard hidden
        if (oldViewHeight < binding.recyclerView.height) {
            binding.gcodeInput.editText.clearFocus()
            Timber.i("Keyboard hidden")
        }
        oldViewHeight = binding.recyclerView.height
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        TerminalFragmentBinding.inflate(inflater, container, false).also { binding = it }.root


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialLayout = true

        // Terminal
        initTerminal(
            if (viewModel.isStyledTerminalUsed()) {
                StyledTerminalAdapter()
            } else {
                PlainTerminalAdapter()
            }
        )
        viewModel.terminalFilters.observe(viewLifecycleOwner, {})

        // Shortcuts & Buttons
        viewModel.uiState.observe(viewLifecycleOwner, Observer(this::updateUi))
        binding.buttonClear.setOnClickListener {
            adapter?.clear()
            viewModel.clear()
        }
        binding.buttonFilters.text = viewModel.selectedTerminalFilters.size.toString()
        binding.buttonFilters.setOnClickListener {
            val availableFilters = (viewModel.terminalFilters.value ?: emptyList()).map {
                Pair(it, viewModel.selectedTerminalFilters.contains(it))
            }

            TerminalFilterDialogFactory().showDialog(
                requireContext(),
                availableFilters
            ) {
                viewModel.selectedTerminalFilters = it.filter { it.second }.map { it.first }
                binding.buttonFilters.text = viewModel.selectedTerminalFilters.size.toString()
                initTerminal(adapter ?: StyledTerminalAdapter())
            }
        }
        binding.buttonToggleStyled.setOnClickListener {
            if (adapter is StyledTerminalAdapter) {
                initTerminal(PlainTerminalAdapter())
                viewModel.setStyledTerminalUsed(false)
            } else {
                initTerminal(StyledTerminalAdapter())
                viewModel.setStyledTerminalUsed(true)
            }
        }

        shortcutLayoutManager = GcodeShortcutLayoutManager(
            layout = binding.buttonList,
            scroller = binding.buttonListScrollView,
            onInsert = ::insertGcode,
            onClicked = { viewModel.executeGcode(it.command) },
        )

        // Gcode input
        binding.gcodeInput.setOnActionListener { sendGcodeFromInput() }
        binding.gcodeInput.editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        binding.gcodeInput.editText.maxLines = 100
        binding.gcodeInput.editText.isSingleLine = false
        binding.gcodeInput.editText.imeOptions = EditorInfo.IME_ACTION_UNSPECIFIED
        binding.gcodeInput.editText.setOnKeyListener { _, i, _ ->
            if (i == KeyEvent.KEYCODE_BACK) {
                binding.gcodeInput.editText.clearFocus()
                true
            } else false
        }
    }

    private fun sendGcodeFromInput() {
        val input = binding.gcodeInput.editText.text.toString().uppercase()
        if (input.isNotBlank()) {
            viewModel.executeGcode(input)
            binding.gcodeInput.editText.text = null
        }
    }

    private fun initTerminal(adapter: TerminalAdapter<*>) {
        binding.recyclerView.adapter = adapter
        observeSerialCommunicationsJob?.cancel()
        observeSerialCommunicationsJob = viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val (old, flow) = viewModel.observeSerialCommunication()

            // Add all items we have so far and scroll to bottom
            adapter.initWithItems(old)
            binding.recyclerView.scrollToPosition(adapter.itemCount - 1)

            // Append all new items
            flow.collect {
                adapter.appendItem(it)
                scrollToBottom()
            }
        }

        binding.buttonToggleStyled.setIconResource(
            if (adapter is StyledTerminalAdapter) {
                R.drawable.ic_round_code_24
            } else {
                R.drawable.ic_round_brush_24
            }
        )

        this.adapter = adapter
    }

    private fun scrollToBottom(forced: Boolean = false) {
        // If we are scrolled to the end, scroll down again after we added the item
        adapter?.let {
            if (forced || !binding.recyclerView.canScrollVertically(1)) {
                wasScrolledToBottom = true
                binding.recyclerView.scrollToPosition(it.getItemCount() - 1)
            }
        }
    }

    private fun updateUi(uiState: TerminalViewModel.UiState) {
        if (!initialLayout) {
            val transition = AutoTransition()
            transition.excludeTarget(binding.recyclerView, true)
            TransitionManager.beginDelayedTransition(view as ViewGroup, transition)
        }

        shortcutLayoutManager.showGcodes(if (uiState.printing) emptyList() else uiState.gcodes)
        binding.gcodeInput.isVisible = !uiState.printing
        binding.printingHint.isVisible = uiState.printing

        // We are not in initial layout anymore as soon as the gcode arrived
        initialLayout = uiState.gcodes.isEmpty()
    }

    private fun insertGcode(gcode: GcodeHistoryItem) {
        binding.gcodeInput.editText.setText(gcode.command)
    }

    override fun onResume() {
        super.onResume()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
        binding.recyclerView.setupWithToolbar(requireOctoActivity())
        binding.recyclerView.viewTreeObserver.addOnGlobalLayoutListener(onLayoutListener)
    }

    override fun onPause() {
        super.onPause()
        binding.recyclerView.viewTreeObserver?.removeOnGlobalLayoutListener(onLayoutListener)
    }
}