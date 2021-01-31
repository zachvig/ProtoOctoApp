package de.crysxd.octoapp.base.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.CallSuper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.ext.findParent
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity


abstract class BaseBottomSheetDialogFragment : BottomSheetDialogFragment() {

    protected abstract val viewModel: BaseViewModel

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.navContoller = findNavController()
        requireOctoActivity().observeErrorEvents(viewModel.errorLiveData)
        requireOctoActivity().observerMessageEvents(viewModel.messages)
        view.setBackgroundResource(R.drawable.bg_bottom_sheet)
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

        // Fix bottom sheet not fully shown on tablet in landscape
        val coordinator = requireView().findParent<CoordinatorLayout>()
        coordinator?.findViewById<View>(R.id.design_bottom_sheet)?.let { bottomSheet ->
            val behaviour = BottomSheetBehavior.from(bottomSheet)
            bottomSheet.doOnLayout {
                behaviour.peekHeight = bottomSheet.height
                coordinator.parent.requestLayout()
            }
        }

        // Limit bottom sheet width to get a pleasant look on tablets

        val maxWidth = requireContext().resources.getDimension(R.dimen.max_bottom_sheet_width).toInt()
        dialog?.window?.setLayout(getScreenWidth().coerceAtMost(maxWidth), ViewGroup.LayoutParams.MATCH_PARENT)
    }

    protected fun getScreenWidth(): Int {
        val wm = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        return metrics.widthPixels
    }
}