package de.crysxd.octoapp.pre_print_controls.ui.widget.extrude

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.crysxd.octoapp.base.ui.widget.OctoWidget
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.injectViewModel
import kotlinx.android.synthetic.main.widget_extrude.*

class ExtrudeWidget(parent: Fragment) : OctoWidget(parent) {

    val viewModel: ExtrudeWidgetViewModel by injectViewModel()

    override fun getTitle(context: Context) = "Extrude"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View =
        inflater.inflate(R.layout.widget_extrude, container, false)

    override fun onViewCreated(view: View) {
        buttonExtrude5.setOnClickListener { viewModel.extrude5mm() }
        buttonExtrude50.setOnClickListener { viewModel.extrude50mm() }
        buttonExtrude100.setOnClickListener { viewModel.extrude100mm() }
        buttonExtrude120.setOnClickListener { viewModel.extrude120mm() }
        buttonExtrudeOther.setOnClickListener { viewModel.extrudeOther(requireContext()) }
    }
}