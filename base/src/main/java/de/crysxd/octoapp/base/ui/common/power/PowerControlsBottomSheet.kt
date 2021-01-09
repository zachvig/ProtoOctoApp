package de.crysxd.octoapp.base.ui.common.power

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.ItemPowerDeviceBinding
import de.crysxd.octoapp.base.databinding.PowerControlsBottomSheetBinding
import de.crysxd.octoapp.base.di.injectViewModel
import de.crysxd.octoapp.base.ui.BaseBottomSheetDialogFragment
import de.crysxd.octoapp.base.ui.common.ViewBindingHolder
import de.crysxd.octoapp.base.ui.ext.findParent
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase
import de.crysxd.octoapp.base.usecase.GetPowerDevicesUseCase.PowerState.*
import de.crysxd.octoapp.octoprint.plugins.power.PowerDevice
import kotlinx.android.parcel.Parcelize
import timber.log.Timber


class PowerControlsBottomSheet : BaseBottomSheetDialogFragment() {

    companion object {
        private const val ARG_ACTION = "action"
        private const val ARG_DEVICE_TYPE = "device_type"
        fun createForAction(action: Action, deviceType: DeviceType = DeviceType.Unspecified) = PowerControlsBottomSheet().also {
            it.arguments = bundleOf(ARG_ACTION to action, ARG_DEVICE_TYPE to deviceType)
        }
    }

    private lateinit var binding: PowerControlsBottomSheetBinding
    private val action get() = requireArguments().getParcelable<Action>(ARG_ACTION)!!
    private val deviceType get() = requireArguments().getParcelable<DeviceType>(ARG_DEVICE_TYPE)!!
    private val adapter by lazy { PowerDeviceAdapter(requireContext()) }
    private var listWasShown = false
    override val viewModel by injectViewModel<PowerControlsViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = PowerControlsBottomSheetBinding.inflate(
        inflater, container, false
    ).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.title.setText(
            when (action) {
                Action.TurnOn -> R.string.power_controls___title_turn_on
                Action.TurnOff -> R.string.power_controls___title_turn_off
                Action.Cycle -> R.string.power_controls___title_cycle
                Action.Unspecified -> R.string.power_controls___title_unspecified
            }
        )
        binding.subtitle.setText(
            when (action) {
                Action.Unspecified -> R.string.power_controls___subtitle_unspecified
                else -> R.string.power_controls___subtitle_specified
            }
        )

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)

        viewModel.setAction(action, deviceType)
        viewModel.viewState.observe(viewLifecycleOwner) {
            when (it) {
                PowerControlsViewModel.ViewState.Loading -> setLoadingActive(true)
                is PowerControlsViewModel.ViewState.Completed -> {
                    (parentFragment as? Parent)?.onPowerDeviceSelected(it.powerDevice, it.action)
                    dismissAllowingStateLoss()
                }
                is PowerControlsViewModel.ViewState.PowerDevicesLoaded -> {
                    adapter.powerDevices = it.powerDevices
                    setLoadingActive(false)
                }
            }
        }
    }


    fun show(fm: FragmentManager) {
        show(fm, "power-controls")
    }

    private fun setLoadingActive(active: Boolean) {
        if (!active) {
            listWasShown = true
        }

        // If the list was visible before, we use invisible to hide so the layout height does not change (animation glitches)
        val hiddenVisibility = if (listWasShown) View.INVISIBLE else View.GONE

        TransitionManager.beginDelayedTransition(requireView().findParent<CoordinatorLayout>())
        binding.progressBar.isVisible = active
        binding.subtitle.visibility = if (!active) View.VISIBLE else hiddenVisibility
        binding.title.visibility = if (!active) View.VISIBLE else hiddenVisibility
        binding.checkboxUseInFuture.visibility = when {
            deviceType is DeviceType.Unspecified -> View.GONE
            active -> hiddenVisibility
            else -> View.VISIBLE
        }
        binding.recyclerView.visibility = if (!active) View.VISIBLE else hiddenVisibility

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

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PowerDeviceViewHolder(parent)

        override fun onBindViewHolder(holder: PowerDeviceViewHolder, position: Int) {
            val item = powerDevices[position]
            holder.binding.name.text = item.first.displayName
            holder.binding.plugin.text = when (item.second) {
                On, Off -> item.first.pluginDisplayName
                Unknown -> getString(R.string.power_controls___x_offline, item.first.pluginDisplayName)
            }
            when (item.second) {
                On -> {
                    holder.binding.icon.setColorFilter(green)
                }
                Unknown, Off -> {
                    holder.binding.icon.setColorFilter(gray)
                }
            }
            holder.itemView.setOnClickListener {
                if (action != Action.Unspecified) {
                    viewModel.executeAction(item.first, action, deviceType, binding.checkboxUseInFuture.isChecked)
                } else {
                    askForAction(item.first)
                }
            }
            holder.itemView.setOnLongClickListener {
                askForAction(item.first)
                true
            }
        }

        override fun getItemCount() = powerDevices.size
    }

    private fun askForAction(device: PowerDevice) {
        val items = arrayOf(
            getString(R.string.power_controls___turn_on),
            getString(R.string.power_controls___turn_off),
            getString(R.string.power_controls___cycle)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setItems(items) { _, which ->
                when (which) {
                    0 -> viewModel.executeAction(device, Action.TurnOn, deviceType, binding.checkboxUseInFuture.isChecked)
                    1 -> viewModel.executeAction(device, Action.TurnOff, deviceType, binding.checkboxUseInFuture.isChecked)
                    2 -> viewModel.executeAction(device, Action.Cycle, deviceType, binding.checkboxUseInFuture.isChecked)
                }
            }
            .show()
    }

    private class PowerDeviceViewHolder(parent: ViewGroup) : ViewBindingHolder<ItemPowerDeviceBinding>
        (ItemPowerDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false))


    sealed class DeviceType : Parcelable {
        @Parcelize
        object PrinterPsu : DeviceType()

        @Parcelize
        object Unspecified : DeviceType()
    }

    sealed class Action : Parcelable {
        @Parcelize
        object TurnOn : Action()

        @Parcelize
        object TurnOff : Action()

        @Parcelize
        object Cycle : Action()

        @Parcelize
        object Unspecified : Action()
    }

    interface Parent {
        fun onPowerDeviceSelected(device: PowerDevice, action: Action?): Any?
    }
}