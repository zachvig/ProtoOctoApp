package de.crysxd.octoapp.base.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.crysxd.octoapp.base.R

open class BottomSheetDialogFragmentCompat : BottomSheetDialogFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.window_background))
    }

    override fun onStart() {
        super.onStart()

        // Fixes dialog hides nav bar on Android O
        if (dialog != null && dialog!!.window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val window = dialog!!.window
            window!!.findViewById<View>(com.google.android.material.R.id.container).fitsSystemWindows = false

            // dark navigation bar icons
            val decorView = window.decorView
            if (!requireContext().resources.getBoolean(R.bool.night_mode)) {
                decorView.systemUiVisibility = decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }

            requireView().updatePadding(
                bottom = (activity?.window?.decorView?.rootWindowInsets?.systemWindowInsetBottom ?: 0)
            )
        }
    }
}