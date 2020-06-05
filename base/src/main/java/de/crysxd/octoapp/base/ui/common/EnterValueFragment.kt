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
import kotlinx.android.synthetic.main.fragment_enter_value.*

class EnterValueFragment : BaseFragment(R.layout.fragment_enter_value) {

    override val viewModel: EnterValueViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = navArgs<EnterValueFragmentArgs>().value
        toolbar.setupWithNavController(findNavController())
        toolbar.title = args.title
        textInputLayout.hint = args.hint
        textInputLayout.editText?.inputType = args.inputType
        textInputLayout.editText?.maxLines = args.maxLines
        textInputLayout.editText?.imeOptions = if(args.maxLines > 1) {
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
        findNavController().popBackStack()
    }
}