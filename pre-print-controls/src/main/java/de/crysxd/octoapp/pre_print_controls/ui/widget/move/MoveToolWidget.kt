package de.crysxd.octoapp.pre_print_controls.ui.widget.move

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.widget_move_tool.*
import java.text.DecimalFormat
import de.crysxd.octoapp.base.R as BaseR

class MoveToolWidget(parent: Fragment) : OctoWidget(parent) {

    val viewModel: MoveToolWidgetViewModel by injectViewModel()

    override fun getTitle(context: Context) = "Move"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.inflate(R.layout.widget_move_tool, container, false)

    override fun onViewCreated(view: View) {
        initJogResolutionSeekBar()
        initControlButtons()
        viewModel.jogResolution.observe(viewLifecycleOwner, Observer {
            val number = DecimalFormat("#.###").format(it)
            textViewJogResolution.text = parent.getString(BaseR.string.x_mm, number)
        })
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
        seekBarJogResolution.max = viewModel.jogResolutionStepsMm.size - 1
        seekBarJogResolution.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                viewModel.jogResolution.postValue(viewModel.jogResolutionStepsMm[progress])
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        seekBarJogResolution.progress = viewModel.jogResolutionStepsMm.size / 2
    }
}