package de.crysxd.octoapp.base.ui.move

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.Observer
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.BaseFragment
import kotlinx.android.synthetic.main.fragment_move_tool_controls.*
import java.text.DecimalFormat

class MoveToolControlsFragment : BaseFragment(R.layout.fragment_move_tool_controls) {

    override val viewModel: MoveToolControlsViewModel by injectViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initJogResolutionSeekBar()
        initControlButtons()
        viewModel.jogResolution.observe(this, Observer {
            val number = DecimalFormat("#.###").format(it)
            textViewJogResolution.text = getString(R.string.x_mm, number)
        })
    }

    private fun initControlButtons() {
        imageButtonHomeZ.setOnClickListener { viewModel.homeZAxis() }
        imageButtonHomeXy.setOnClickListener { viewModel.homeXYAxis() }
        imageButtonMoveXPositive.setOnClickListener { viewModel.jog(x = MoveToolControlsViewModel.Direction.Positive) }
        imageButtonMoveXNegative.setOnClickListener { viewModel.jog(x = MoveToolControlsViewModel.Direction.Negative) }
        imageButtonMoveYPositive.setOnClickListener { viewModel.jog(y = MoveToolControlsViewModel.Direction.Positive) }
        imageButtonMoveYNegative.setOnClickListener { viewModel.jog(y = MoveToolControlsViewModel.Direction.Negative) }
        imageButtonMoveZPositive.setOnClickListener { viewModel.jog(z = MoveToolControlsViewModel.Direction.Positive) }
        imageButtonMoveZNegative.setOnClickListener { viewModel.jog(z = MoveToolControlsViewModel.Direction.Negative) }
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