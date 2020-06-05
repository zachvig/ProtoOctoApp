package de.crysxd.octoapp.base.ui.common

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.setupWithNavController
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.BaseFragment
import de.crysxd.octoapp.base.ui.ext.clearFocusAndHideSoftKeyboard
import de.crysxd.octoapp.base.ui.ext.requestFocusAndOpenSoftKeyboard
import de.crysxd.octoapp.base.ui.navigation.NavigationResultMediator
import kotlinx.android.synthetic.main.fragment_enter_value.*

class EnterValueFragment : BaseFragment(R.layout.fragment_enter_value) {

    override val viewModel: EnterValueViewModel by injectViewModel()

    private val navArgs: EnterValueFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setupWithNavController(findNavController())
        toolbar.title = navArgs.title
        textInputLayout.hint = navArgs.hint
        textInputLayout.editText?.inputType = navArgs.inputType
        textInputLayout.editText?.maxLines = navArgs.maxLines
        textInputLayout.editText?.setText(navArgs.value)
        textInputLayout.editText?.imeOptions = if(navArgs.maxLines > 1) {
            EditorInfo.IME_ACTION_NONE
        } else {
            EditorInfo.IME_ACTION_DONE
        }

        textInputLayout.editText?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_NONE) {
                navigateBackWithResult()
                true
            } else {
                false
            }
        }

        textInputLayout.editText?.requestFocusAndOpenSoftKeyboard()

        toolbar.inflateMenu(R.menu.menu_enter_value)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menuItemDone -> {
                    navigateBackWithResult()
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateBackWithResult() {
        textInputLayout.editText?.clearFocusAndHideSoftKeyboard()
        NavigationResultMediator.postResult(navArgs.resultId, textInputLayout.editText?.text?.toString() ?: "")
        findNavController().popBackStack()
    }
}