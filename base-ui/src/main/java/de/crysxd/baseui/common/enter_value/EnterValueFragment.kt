package de.crysxd.baseui.common.enter_value

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.R
import de.crysxd.baseui.common.OctoToolbar
import de.crysxd.baseui.databinding.EnterValueFragmentBinding
import de.crysxd.baseui.di.injectViewModel
import de.crysxd.baseui.ext.clearFocusAndHideSoftKeyboard
import de.crysxd.baseui.ext.requestFocusAndOpenSoftKeyboard
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.utils.NavigationResultMediator
import kotlinx.parcelize.Parcelize

class EnterValueFragment : BaseFragment() {

    override val viewModel: EnterValueViewModel by injectViewModel()
    private lateinit var binding: EnterValueFragmentBinding

    private val navArgs: EnterValueFragmentArgs by navArgs()

    private var resultPosted = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        EnterValueFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSet.setOnClickListener { navigateBackWithResult() }
        binding.buttonSet.text = navArgs.action ?: getString(android.R.string.ok)
        binding.textViewTitle.text = navArgs.title
        binding.textInputLayout.hintNormal = navArgs.hint
        binding.textInputLayout.editText.inputType = navArgs.inputType
        binding.textInputLayout.editText.maxLines = navArgs.maxLines
        binding.textInputLayout.editText.setText(navArgs.value)
        binding.textInputLayout.editText.imeOptions = if (navArgs.maxLines > 1) {
            EditorInfo.IME_ACTION_NONE
        } else {
            EditorInfo.IME_ACTION_DONE
        }

        binding.textInputLayout.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_NONE) {
                navigateBackWithResult()
                true
            } else {
                false
            }
        }

        if (navArgs.selectAll) {
            binding.textInputLayout.editText.selectAll()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.textInputLayout.editText.requestFocusAndOpenSoftKeyboard()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
    }

    override fun onPause() {
        super.onPause()
        binding.textInputLayout.editText.clearFocusAndHideSoftKeyboard()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cancelled, post null result
        if (!resultPosted) {
            NavigationResultMediator.postResult(navArgs.resultId, null)
        }
    }

    private fun navigateBackWithResult() {
        val result = binding.textInputLayout.editText.text?.toString() ?: ""
        val error = (navArgs.validator ?: NotEmptyValidator()).validate(requireContext(), result)
        binding.textInputLayout.error = error

        if (error == null) {
            resultPosted = true
            binding.textInputLayout.editText.clearFocusAndHideSoftKeyboard()
            NavigationResultMediator.postResult(navArgs.resultId, result)
            findNavController().popBackStack()
        }
    }

    interface ValueValidator : Parcelable {
        fun validate(context: Context, value: String): String?
    }

    @Parcelize
    class NotEmptyValidator : ValueValidator {
        override fun validate(context: Context, value: String) = if (value.isBlank()) {
            context.getString(R.string.error_please_enter_a_value)
        } else {
            null
        }
    }
}