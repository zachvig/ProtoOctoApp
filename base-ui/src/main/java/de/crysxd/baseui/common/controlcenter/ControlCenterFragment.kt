package de.crysxd.baseui.common.controlcenter

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import de.crysxd.baseui.BaseFragment
import de.crysxd.baseui.R
import de.crysxd.baseui.databinding.ControlCenterFragmentBinding
import de.crysxd.baseui.databinding.ControleCenterItemBinding
import de.crysxd.baseui.di.injectViewModel
import de.crysxd.baseui.ext.requireOctoActivity
import de.crysxd.baseui.utils.colorTheme
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import timber.log.Timber

class ControlCenterFragment : BaseFragment() {
    override val viewModel by injectViewModel<ControlCenterViewModel>()
    private lateinit var binding: ControlCenterFragmentBinding
    private val formatEtaUseCase by lazy { BaseInjector.get().formatEtaUseCase() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ControlCenterFragmentBinding.inflate(inflater, container, false).also {
            binding = it
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("CREATE")
        requireOctoActivity().applyInsetsToView(binding.root)

        binding.imageButton.setOnClickListener {
            Toast.makeText(requireContext(), "Hello world!", Toast.LENGTH_SHORT).show()
        }

        viewModel.viewState.observe(viewLifecycleOwner) {
            viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                bindList(it.instances.sortedBy { i -> i.info.colorTheme.order }, it.activeId)
            }
        }
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
        binding.activate.setOnClickListener { if (!isActive) viewModel.active(instance) }
        binding.activate.backgroundTintList = ColorStateList.valueOf(instance.info.colorTheme.dark).takeIf { isActive }
        binding.activate.setImageResource(if (isActive) R.drawable.ic_round_check_24 else R.drawable.ic_round_swap_horiz_24)

        // Colors
        binding.colorStrip.setBackgroundColor(instance.info.colorTheme.dark)
        binding.progress.progressTintList = ColorStateList.valueOf(instance.info.colorTheme.dark)

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
                    "Idle"
                }

                else -> {
                    binding.detail2.text = ""
                    binding.percent.text = ""
                    "No printer connected"
                }
            }
        } else {
            "Connecting..."
        }
    }

    private fun formatEta(secsLeft: Int?) = secsLeft?.toLong()?.let {
        formatEtaUseCase.executeBlocking(FormatEtaUseCase.Params(secsLeft = it, useCompactDate = false, showLabel = true, allowRelative = true))
    }

    private fun createItem() = ControleCenterItemBinding.inflate(LayoutInflater.from(requireContext()), binding.list, false).apply {
//        webcam.smallMode = true
//        webcam.canSwitchWebcam = false
        content.clipToOutline = true
        Timber.i("Create new item")
    }

    override fun onStart() {
        super.onStart()
        Timber.i("START")
    }

    override fun onStop() {
        super.onStop()
        Timber.i("STOP")
    }
}
