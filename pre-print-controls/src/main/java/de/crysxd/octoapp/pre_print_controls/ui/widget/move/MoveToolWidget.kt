package de.crysxd.octoapp.pre_print_controls.ui.widget.move

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.CompoundButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas
import androidx.lifecycle.LifecycleOwner
import de.crysxd.octoapp.base.ui.widget.RecyclableOctoWidget
import de.crysxd.octoapp.base.ui.widget.WidgetHostFragment
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.databinding.MoveToolWidgetBinding
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel

class MoveToolWidget(context: Context) : RecyclableOctoWidget<MoveToolWidgetBinding, MoveToolWidgetViewModel>(context) {

    override val binding: MoveToolWidgetBinding = MoveToolWidgetBinding.inflate(LayoutInflater.from(context))
    private val jogResolutionButtons = listOf(
        binding.buttonJogResolution0025,
        binding.buttonJogResolution01,
        binding.buttonJogResolution1,
        binding.buttonJogResolution10,
        binding.buttonJogResolution100
    )

    override fun onResume(lifecycleOwner: LifecycleOwner) {
        super.onResume(lifecycleOwner)
        initControlButtons()
        initJogResolutionSeekBar(context)
    }

    override fun createNewViewModel(parent: WidgetHostFragment) = parent.injectViewModel<MoveToolWidgetViewModel>().value

    override fun getTitle(context: Context) = context.getString(R.string.widget_move)
    override fun getAnalyticsName() = "move"
    override fun getActionIcon() = R.drawable.ic_round_settings_24
    override fun onAction() {
        baseViewModel.showSettings(parent.requireContext())
    }

    private fun initControlButtons() {
        binding.imageButtonHomeZ.setOnClickListener { recordInteraction(); baseViewModel.homeZAxis() }
        binding.imageButtonHomeXy.setOnClickListener { recordInteraction(); baseViewModel.homeXYAxis() }
        binding.imageButtonMoveXPositive.setOnClickListener { recordInteraction(); baseViewModel.jog(x = MoveToolWidgetViewModel.Direction.Positive) }
        binding.imageButtonMoveXNegative.setOnClickListener { recordInteraction(); baseViewModel.jog(x = MoveToolWidgetViewModel.Direction.Negative) }
        binding.imageButtonMoveYPositive.setOnClickListener { recordInteraction(); baseViewModel.jog(y = MoveToolWidgetViewModel.Direction.Positive) }
        binding.imageButtonMoveYNegative.setOnClickListener { recordInteraction(); baseViewModel.jog(y = MoveToolWidgetViewModel.Direction.Negative) }
        binding.imageButtonMoveZPositive.setOnClickListener { recordInteraction(); baseViewModel.jog(z = MoveToolWidgetViewModel.Direction.Positive) }
        binding.imageButtonMoveZNegative.setOnClickListener { recordInteraction(); baseViewModel.jog(z = MoveToolWidgetViewModel.Direction.Negative) }
    }

    private fun initJogResolutionSeekBar(context: Context) {
        jogResolutionButtons.forEach { it.setOnCheckedChangeListener(this::onJogResolutionChanged) }

        val diameterDpRange = 12f..28f
        val step = (diameterDpRange.endInclusive - diameterDpRange.start) / jogResolutionButtons.size
        jogResolutionButtons.forEachIndexed { i, it ->
            it.background = createJogResolutionButtonBackground(context, diameterDpRange.start + step * i)
        }

        binding.jogResolutionGroup.setOnClickListener { }

        val checkedId = when (baseViewModel.jogResolution) {
            0.025f -> R.id.buttonJogResolution0025
            0.1f -> R.id.buttonJogResolution01
            1f -> R.id.buttonJogResolution1
            10f -> R.id.buttonJogResolution10
            100f -> R.id.buttonJogResolution100
            else -> {
                baseViewModel.jogResolution = 10f
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

        binding.jogResolutionGroup.performClick()

        when (jogResolutionButtons.first { it.isChecked }.id) {
            R.id.buttonJogResolution0025 -> baseViewModel.jogResolution = 0.025f
            R.id.buttonJogResolution01 -> baseViewModel.jogResolution = 0.1f
            R.id.buttonJogResolution1 -> baseViewModel.jogResolution = 1f
            R.id.buttonJogResolution10 -> baseViewModel.jogResolution = 10f
            R.id.buttonJogResolution100 -> baseViewModel.jogResolution = 100f
        }
    }

    private fun createJogResolutionButtonBackground(context: Context, diameterDp: Float): Drawable {
        val diameterPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, diameterDp, context.resources.displayMetrics).toInt()
        val bitmap = Bitmap.createBitmap(diameterPx, diameterPx, Bitmap.Config.ARGB_8888)
        val circle = ContextCompat.getDrawable(context, R.drawable.circle)
        circle?.setTint(ContextCompat.getColor(context, R.color.accent))
        circle?.setBounds(0, 0, diameterPx, diameterPx)
        bitmap.applyCanvas { circle?.draw(this) }
        return BitmapDrawable(context.resources, bitmap).also {
            it.gravity = Gravity.CENTER
        }
    }
}
