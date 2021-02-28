package de.crysxd.octoapp.base.ui.base

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.CallSuper
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnLayout
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

        // Fix bottom sheet not fully shown on tablet in landscape
        forceResizeBottomSheet()

        // Limit bottom sheet width to get a pleasant look on tablets
        val maxWidth = requireContext().resources.getDimension(R.dimen.max_bottom_sheet_width).toInt()
        dialog?.window?.setLayout(getScreenWidth().coerceAtMost(maxWidth), ViewGroup.LayoutParams.MATCH_PARENT)
    }

    fun forceResizeBottomSheet() {
        // This is a fix for tablets in landscape mode not fully showing a bottom sheet
        // We manually set the peek height to make sure everything is shown
        val coordinator = requireView().findParent<CoordinatorLayout>()
        coordinator?.findViewById<View>(R.id.design_bottom_sheet)?.let { bottomSheet ->
            val behaviour = BottomSheetBehavior.from(bottomSheet)
            bottomSheet.doOnLayout {
                behaviour.peekHeight = bottomSheet.height
                coordinator.parent.requestLayout()
            }
        }
    }

    protected fun getScreenWidth(): Int {
        val wm = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        return metrics.widthPixels
    }
}