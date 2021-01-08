package de.crysxd.octoapp.base.ui.common.power

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.ItemPowerDeviceBinding
import de.crysxd.octoapp.base.databinding.PowerControlsBottomSheetBinding
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.BottomSheetDialogFragmentCompat
import de.crysxd.octoapp.base.ui.common.ViewBindingHolder
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase.PowerState.*
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.android.parcel.Parcelize
import timber.log.Timber

class PowerControlsBottomSheet : BottomSheetDialogFragmentCompat() {

    companion object {
        private const val ARG_ACTION = "action"
        fun createForAction(action: Action) = PowerControlsBottomSheet().also {
            it.arguments = bundleOf(ARG_ACTION to action)
        }
    }

    private lateinit var binding: PowerControlsBottomSheetBinding
    private val action get() = requireArguments().getParcelable<Action>(ARG_ACTION)!!
    private val adapter by lazy { PowerDeviceAdapter(requireContext()) }
    private val viewModel by injectViewModel<PowerControlsViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = PowerControlsBottomSheetBinding.inflate(
        inflater, container, false
    ).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, true)
        viewModel.powerDevices.observe(viewLifecycleOwner) {
            adapter.powerDevices = it
        }
    }

    private inner class PowerDeviceAdapter(context: Context) : RecyclerView.Adapter<PowerDeviceViewHolder>() {

        private val green = ContextCompat.getColor(context, R.color.primary_dark)
        private val gray = ContextCompat.getColor(context, R.color.light_text)
        var powerDevices: List<Pair<PowerDevice, GetPowerDevicesUseCase.PowerState>> = emptyList()
            set(value) {
                Timber.i("Devices: ${value.map { it.first.displayName }}")
                field = value.sortedBy { it.first.displayName }
                notifyDataSetChanged()
            }

        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int) = powerDevices[position].first.let {
            it.displayName + it.pluginDisplayName
        }.hashCode().toLong()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PowerDeviceViewHolder(parent)

        override fun onBindViewHolder(holder: PowerDeviceViewHolder, position: Int) {
            val item = powerDevices[position]
            holder.binding.name.text = item.first.displayName
            holder.binding.plugin.text = item.first.pluginDisplayName
            holder.binding.icon.alpha = if (item.second is Loading) 0f else 1f
            when (item.second) {
                On -> {
                    holder.binding.icon.setColorFilter(green)
                    holder.binding.icon.setImageResource(R.drawable.ic_round_power_settings_new_24)
                }
                Off -> {
                    holder.binding.icon.setColorFilter(gray)
                    holder.binding.icon.setImageResource(R.drawable.ic_round_power_settings_new_24)
                }
                Unknown -> {
                    holder.binding.icon.setColorFilter(gray)
                    holder.binding.icon.setImageResource(R.drawable.ic_round_help_outline_24)
                }
                Loading -> Unit
            }
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