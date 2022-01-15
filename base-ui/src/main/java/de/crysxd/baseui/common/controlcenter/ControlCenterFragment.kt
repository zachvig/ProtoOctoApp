package de.crysxd.baseui.common.controlcenter

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.R
import de.crysxd.baseui.databinding.ControlCenterFragmentBinding
import de.crysxd.baseui.databinding.ControleCenterItemBinding
import de.crysxd.baseui.di.injectActivityViewModel
import de.crysxd.baseui.ext.findParent
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.menu.MenuBottomSheetFragment
import de.crysxd.baseui.menu.switchprinter.SwitchOctoPrintMenu
import de.crysxd.baseui.utils.colorTheme
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class ControlCenterFragment : BaseFragment() {
    override val viewModel by injectActivityViewModel<ControlCenterViewModel>()
    private lateinit var binding: ControlCenterFragmentBinding
    private val formatEtaUseCase by lazy { BaseInjector.get().formatEtaUseCase() }
    private val placeholderDrawable = ColorDrawable(Color.BLACK)
    private var rippleDrawable: RippleDrawable? = null
    private var rippleJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        (viewModel.viewPool ?: ControlCenterFragmentBinding.inflate(inflater, container, false)).also {
            viewModel.viewPool = it
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireOctoActivity().applyInsetsToView(binding.root)

        binding.imageButton.setOnClickListener {
            MenuBottomSheetFragment.createForMenu(SwitchOctoPrintMenu()).show(childFragmentManager)
        }

        viewModel.viewState.observe(viewLifecycleOwner) {
            viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                bindList(it.instances.sortedBy { i -> i.info.colorTheme.order }, it.activeId)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (binding.root.parent as? ViewGroup)?.removeView(binding.root)
//        binding.list.children.forEach { v -> ControleCenterItemBinding.bind(v).resetWebcam() }
    }

    private fun bindList(instances: List<ControlCenterViewModel.Instance>, activeId: String?) {
        val previousViews = binding.list.children.map { ControleCenterItemBinding.bind(it) }.toMutableList()
        val countChanged = instances.size != previousViews.size
        if (countChanged) {
            previousViews.forEach { binding.list.removeView(it.root) }
        }

        instances.forEach {
            val item = if (previousViews.isNotEmpty()) previousViews.removeAt(0) else createItem()
            bindInstance(item, it, it.info.id == activeId)
            if (item.root.parent == null) {
                binding.list.addView(item.root)
            }
        }
    }

    private fun bindInstance(binding: ControleCenterItemBinding, instance: ControlCenterViewModel.Instance, isActive: Boolean) {
        binding.label.text = instance.info.label

        // Button
        binding.root.setOnClickListener { if (!isActive) activate(instance, it) }
        binding.activate.backgroundTintList = ColorStateList.valueOf(instance.info.colorTheme.dark).takeIf { isActive }
        binding.activate.setImageResource(if (isActive) R.drawable.ic_round_check_24 else R.drawable.ic_round_swap_horiz_24)

        // Colors
        binding.colorStrip.setBackgroundColor(instance.info.colorTheme.dark)
        binding.progress.progressTintList = ColorStateList.valueOf(instance.info.colorTheme.dark)

        // Webcam
        if (instance.snapshot != null) {
            if (binding.webcam.drawable == placeholderDrawable) {
                binding.webcamOverlay.animate().alpha(0f).start()
            }
            binding.webcam.setImageBitmap(instance.snapshot)
        } else {
            binding.resetWebcam()
        }

        // Texts
        binding.detail1.text = if (instance.lastMessage != null) {
            when {
                instance.lastMessage.state?.flags?.isPrinting() == true -> {
                    binding.percent.text = instance.lastMessage.progress?.completion?.let { getString(R.string.x_percent, it) }
                    binding.progress.progress = (instance.lastMessage.progress?.completion ?: 0f).toInt()
                    binding.detail2.text = formatEta(instance.lastMessage.progress?.printTimeLeft)
                    instance.lastMessage.job?.file?.display
                }

                instance.lastMessage.state?.flags?.isOperational() == true -> {
                    binding.detail2.text = ""
                    binding.percent.text = ""
                    getString(R.string.app_widget___idle, instance.info.label)
                }

                else -> {
                    binding.detail2.text = ""
                    binding.percent.text = ""
                    getString(R.string.app_widget___no_printer)
                }
            }
        } else {
            getString(R.string.app_widget___no_data)
        }
    }

    private fun activate(instance: ControlCenterViewModel.Instance, trigger: View) {
        rippleDrawable = RippleDrawable(ColorStateList.valueOf(instance.info.colorTheme.dark), null, null).also {
            binding.rippleView.background = it
            it.state = intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled)

            val backgroundLocation = IntArray(2)
            val triggerLocation = IntArray(2)
            trigger.getLocationInWindow(triggerLocation)
            binding.rippleView.getLocationInWindow(backgroundLocation)
            val x = triggerLocation.first() - backgroundLocation.first()
            val y = triggerLocation.last() - backgroundLocation.last()
            it.setHotspot(x.toFloat(), y.toFloat())

            rippleJob = viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                delay(300)
                viewModel.active(instance)
                delay(200)
                it.state = intArrayOf()
                requireView().findParent<ControlCenterHostLayout>()?.dismiss()
            }
        }
    }

    private fun formatEta(secsLeft: Int?) = secsLeft?.toLong()?.let {
        formatEtaUseCase.executeBlocking(FormatEtaUseCase.Params(secsLeft = it, useCompactDate = false, showLabel = true, allowRelative = true))
    }

    private fun ControleCenterItemBinding.resetWebcam() {
        webcam.setImageDrawable(placeholderDrawable)
        webcamOverlay.alpha = 1f
    }

    private fun createItem() = ControleCenterItemBinding.inflate(LayoutInflater.from(requireContext()), binding.list, false).apply {
        resetWebcam()
        content.clipToOutline = true
    }
}
