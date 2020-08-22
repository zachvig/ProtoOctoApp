package de.crysxd.octoapp.pre_print_controls.ui.widget.move

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.fragment.app.Fragment
import de.crysxd.octoapp.base.ui.ext.suspendedInflate
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.widget_move_tool.*

class MoveToolWidget(parent: Fragment) : OctoWidget(parent) {

    val viewModel: MoveToolWidgetViewModel by injectViewModel()
    val jogResolutionButtons by lazy {
        listOf(
            buttonJogResolution0025,
            buttonJogResolution01,
            buttonJogResolution1,
            buttonJogResolution10,
            buttonJogResolution100
        )
    }

    override fun getTitle(context: Context) = "Move"

    override suspend fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.suspendedInflate(R.layout.widget_move_tool, container, false)

    override fun onViewCreated(view: View) {
        initJogResolutionSeekBar()
        initControlButtons()
    }

    private fun initControlButtons() {
        imageButtonHomeZ.setOnClickListener { viewModel.homeZAxis() }
        imageButtonHomeXy.setOnClickListener { viewModel.homeXYAxis() }
        imageButtonMoveXPositive.setOnClickListener { viewModel.jog(x = MoveToolWidgetViewModel.Direction.Positive) }
        imageButtonMoveXNegative.setOnClickListener { viewModel.jog(x = MoveToolWidgetViewModel.Direction.Negative) }
        imageButtonMoveYPositive.setOnClickListener { viewModel.jog(y = MoveToolWidgetViewModel.Direction.Positive) }
        imageButtonMoveYNegative.setOnClickListener { viewModel.jog(y = MoveToolWidgetViewModel.Direction.Negative) }
        imageButtonMoveZPositive.setOnClickListener { viewModel.jog(z = MoveToolWidgetViewModel.Direction.Positive) }
        imageButtonMoveZNegative.setOnClickListener { viewModel.jog(z = MoveToolWidgetViewModel.Direction.Negative) }
    }

    private fun initJogResolutionSeekBar() {
        jogResolutionButtons.forEach { it.setOnCheckedChangeListener(this::onJogResolutionChanged) }

        val diameterDpRange = 12f..28f
        val step = (diameterDpRange.endInclusive - diameterDpRange.start) / jogResolutionButtons.size
        jogResolutionButtons.forEachIndexed { i, it ->
            it.background = createJogResolutionButtonBackground(diameterDpRange.start + step * i)
        }

        jogResolutionGroup.setOnClickListener { }

        val checkedId = when (viewModel.jogResolution) {
            0.025f -> R.id.buttonJogResolution0025
            0.1f -> R.id.buttonJogResolution01
            1f -> R.id.buttonJogResolution1
            10f -> R.id.buttonJogResolution10
            100f -> R.id.buttonJogResolution100
            else -> {
                viewModel.jogResolution = 10f
                R.id.buttonJogResolution10
            }
        }

        onJogResolutionChanged(view.findViewById(checkedId), true)
    }

    private fun onJogResolutionChanged(view: CompoundButton, checked: Boolean) {
        if (checked) {
            jogResolutionButtons.filter {
                it != view
            }.forEach {
                it.isChecked = false
                it.background?.alpha = 255
            }

            view.background?.alpha = 0
        }

        if (!jogResolutionButtons.any { it.isChecked }) {
            view.isChecked = true
        }

        jogResolutionGroup.performClick()

        when (jogResolutionButtons.first { it.isChecked }.id) {
            R.id.buttonJogResolution0025 -> viewModel.jogResolution = 0.025f
            R.id.buttonJogResolution01 -> viewModel.jogResolution = 0.1f
            R.id.buttonJogResolution1 -> viewModel.jogResolution = 1f
            R.id.buttonJogResolution10 -> viewModel.jogResolution = 10f
            R.id.buttonJogResolution100 -> viewModel.jogResolution = 100f
        }
    }

    private fun createJogResolutionButtonBackground(diameterDp: Float): Drawable {
        val diameterPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, diameterDp, requireContext().resources.displayMetrics).toInt()
        val bitmap = Bitmap.createBitmap(diameterPx, diameterPx, Bitmap.Config.ARGB_8888)
        val circle = ContextCompat.getDrawable(requireContext(), R.drawable.circle)
        circle?.setTint(ContextCompat.getColor(requireContext(), R.color.accent))
        circle?.setBounds(0, 0, diameterPx, diameterPx)
        bitmap.applyCanvas { circle?.draw(this) }
        return BitmapDrawable(requireContext().resources, bitmap).also {
            it.gravity = Gravity.CENTER
        }
    }
}