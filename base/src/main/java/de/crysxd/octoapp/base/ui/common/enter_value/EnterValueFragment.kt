package de.crysxd.octoapp.base.ui.common.enter_value

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.common.OctoToolbar
import de.crysxd.octoapp.base.ui.ext.clearFocusAndHideSoftKeyboard
import de.crysxd.octoapp.base.ui.ext.requestFocusAndOpenSoftKeyboard
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_enter_value.*

class EnterValueFragment : BaseFragment(R.layout.fragment_enter_value) {

    override val viewModel: EnterValueViewModel by injectViewModel()

    private val navArgs: EnterValueFragmentArgs by navArgs()

    private var resultPosted = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonSet.setOnClickListener { navigateBackWithResult() }
        buttonSet.text = navArgs.action ?: getString(android.R.string.ok)
        textViewTitle.text = navArgs.title
        textInputLayout.hintNormal = navArgs.hint
        textInputLayout.editText.inputType = navArgs.inputType
        textInputLayout.editText.maxLines = navArgs.maxLines
        textInputLayout.editText.setText(navArgs.value)
        textInputLayout.editText.imeOptions = if (navArgs.maxLines > 1) {
            EditorInfo.IME_ACTION_NONE
        } else {
            EditorInfo.IME_ACTION_DONE
        }

        textInputLayout.editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_NONE) {
                navigateBackWithResult()
                true
            } else {
                false
            }
        }

        if (navArgs.selectAll) {
            textInputLayout?.editText?.selectAll()
        }
    }

    override fun onResume() {
        super.onResume()
        textInputLayout.editText.requestFocusAndOpenSoftKeyboard()
        requireOctoActivity().octoToolbar.state = OctoToolbar.State.Hidden
    }

    override fun onPause() {
        super.onPause()
        textInputLayout.editText.clearFocusAndHideSoftKeyboard()
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cancelled, post null result
        if (!resultPosted) {
            NavigationResultMediator.postResult(navArgs.resultId, null)
        }
    }

    private fun navigateBackWithResult() {
        val result = textInputLayout.editText.text?.toString() ?: ""
        val error = (navArgs.validator ?: NotEmptyValidator()).validate(requireContext(), result)
        textInputLayout.error = error

        if (error == null) {
            textInputLayout.editText.clearFocusAndHideSoftKeyboard()

            resultPosted = true
            requireView().postDelayed({
                NavigationResultMediator.postResult(navArgs.resultId, result)
                findNavController().popBackStack()
            }, 300)
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