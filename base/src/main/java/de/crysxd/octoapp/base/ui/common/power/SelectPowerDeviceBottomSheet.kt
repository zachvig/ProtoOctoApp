package de.crysxd.octoapp.base.ui.common.power

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.crysxd.octoapp.base.databinding.ItemPowerDeviceBinding
import de.crysxd.octoapp.base.databinding.SelectPowerDeviceBottomSheetBinding
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.common.ViewBindingHolder
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.android.parcel.Parcelize
import timber.log.Timber

class SelectPowerDeviceBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_ACTION = "action"
        fun createForAction(action: Action) = SelectPowerDeviceBottomSheet().also {
            it.arguments = bundleOf(ARG_ACTION to action)
        }
    }

    private lateinit var binding: SelectPowerDeviceBottomSheetBinding
    private val action get() = requireArguments().getParcelable<Action>(ARG_ACTION)!!
    private val adapter = PowerDeviceAdapter()
    private val viewModel by injectViewModel<SelectPowerDeviceViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = SelectPowerDeviceBottomSheetBinding.inflate(
        inflater, container, false
    ).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.adapter = adapter
        viewModel.powerDevices.observe(viewLifecycleOwner) {
            adapter.powerDevices = it
        }
    }

    private inner class PowerDeviceAdapter : RecyclerView.Adapter<PowerDeviceViewHolder>() {

        var powerDevices: List<Pair<PowerDevice, Boolean?>> = emptyList()
            set(value) {
                Timber.i("Devices: ${value.map { it.first.displayName }}")
                field = value.sortedBy { it.first.displayName }
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PowerDeviceViewHolder(parent)

        override fun onBindViewHolder(holder: PowerDeviceViewHolder, position: Int) {
            val item = powerDevices[position]
            val text = listOfNotNull(item.first.displayName, item.second?.toString()).joinToString()
            holder.binding.text.text = text
            holder.itemView.setOnClickListener {
                (parentFragment as? Parent)?.onPowerDeviceSelected(item.first, action)
                dismiss()
            }
        }

        override fun getItemCount() = powerDevices.size
    }

    private class PowerDeviceViewHolder(parent: ViewGroup) : ViewBindingHolder<ItemPowerDeviceBinding>
        (ItemPowerDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    sealed class Action : Parcelable {
        @Parcelize
        object TurnOn : Action()

        @Parcelize
        object TurnOff : Action()

        @Parcelize
        object Cycle : Action()
    }

    interface Parent {
        fun onPowerDeviceSelected(device: PowerDevice, action: Action): Any
    }
}