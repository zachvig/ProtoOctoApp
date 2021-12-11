package de.crysxd.octoapp.filemanager.ui.file_details

import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Callback
import de.crysxd.baseui.di.BaseUiInjector
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.base.ext.format
import de.crysxd.octoapp.filemanager.R
import de.crysxd.octoapp.filemanager.databinding.InfoTabFragmentBinding
import de.crysxd.octoapp.filemanager.di.FileManagerInjector
import de.crysxd.octoapp.filemanager.di.injectParentViewModel
import de.crysxd.octoapp.filemanager.ui.CropAlphaTransformation
import de.crysxd.octoapp.octoprint.models.files.FileOrigin
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class InfoTabFragment : Fragment() {

    private val labels = mutableListOf<View>()
    private val viewModel: FileDetailsViewModel by injectParentViewModel(FileManagerInjector.get().viewModelFactory())
    private val margin0 by lazy { requireContext().resources.getDimensionPixelSize(R.dimen.margin_0) }
    private val margin2 by lazy { requireContext().resources.getDimensionPixelSize(R.dimen.margin_2) }
    private val margin3 by lazy { requireContext().resources.getDimensionPixelSize(R.dimen.margin_3) }
    private lateinit var binding: InfoTabFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        InfoTabFragmentBinding.inflate(inflater, container, false).also { binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val file = viewModel.file
            val formatDurationUseCase = de.crysxd.octoapp.base.di.BaseInjector.get().formatDurationUseCase()

            // Load preview image
            val start = System.currentTimeMillis()
            file.thumbnail?.let {
                BaseUiInjector.get().picasso().observe(viewLifecycleOwner) { picasso ->
                    picasso.load(it)
                        .noFade()
                        .transform(CropAlphaTransformation())
                        .into(binding.preview, object : Callback {
                            override fun onError(e: Exception?) = Unit
                            override fun onSuccess() {
                                binding.preview.post {
                                    if (start - System.currentTimeMillis() > 30) {
                                        TransitionManager.beginDelayedTransition(view as ViewGroup)
                                    }
                                    binding.preview.isVisible = true

                                    // Limit to 16:9 at most
                                    binding.preview.measure(
                                        View.MeasureSpec.makeMeasureSpec(binding.generatedContent.width, View.MeasureSpec.EXACTLY),
                                        View.MeasureSpec.makeMeasureSpec((binding.generatedContent.width * (9 / 16f)).roundToInt(), View.MeasureSpec.AT_MOST),
                                    )
                                    binding.preview.updateLayoutParams {
                                        height = binding.preview.measuredHeight
                                    }
                                }
                            }
                        })
                }
            }

            // Bind data
            binding.printName.text = file.name
            addTitle(R.string.file_manager___file_details___print_info)
            addDetail(
                label = R.string.file_manager___file_details___print_time,
                value = file.gcodeAnalysis?.estimatedPrintTime?.toLong()?.let { formatDurationUseCase.execute(it) }
            )
            addDetail(
                label = R.string.file_manager___file_details___model_size,
                value = file.gcodeAnalysis?.dimensions?.let { String.format(Locale.getDefault(), "%.1f × %.1f × %.1f mm", it.width, it.depth, it.height) }
            )
            addDetail(
                label = R.string.file_manager___file_details___filament_use,
                value = file.gcodeAnalysis?.filament?.let {
                    val totalLength = listOfNotNull(it.tool0, it.tool1).sumOf { s -> s.length }
                    val totalVolume = listOfNotNull(it.tool0, it.tool1).sumOf { s -> s.volume }
                    String.format(Locale.getDefault(), "%.02f m / %.02f cm³", totalLength / 1000, totalVolume)
                }
            )

            addTitle(R.string.file_manager___file_details___file)
            addDetail(
                label = R.string.location,
                value = when (file.origin) {
                    FileOrigin.SdCard -> getString(R.string.file_manager___file_details___file_location_sd_card)
                    FileOrigin.Local -> getString(R.string.file_manager___file_details___file_location_local)
                    else -> getString(R.string.file_manager___file_details___file_location_unknown)
                }
            )
            addDetail(
                label = R.string.file_manager___file_details___path,
                value = "/" + file.path?.removeSuffix(file.name ?: "")?.removeSuffix("/")
            )
            addDetail(
                label = R.string.file_manager___file_details___file_size,
                value = file.size.asStyleFileSize()
            )
            addDetail(
                label = R.string.file_manager___file_details___uploaded,
                value = Date(file.date * 1000).format()
            )

            addTitle(R.string.file_manager___file_details___history)
            addDetail(
                label = R.string.file_manager___file_details___last_print,
                value = file.prints?.last?.let {
                    getString(
                        if (it.success) R.string.file_manager___file_details___last_print_at_x_success else R.string.file_manager___file_details___last_print_at_x_failure,
                        Date(it.date.toLong() * 1000).format()
                    )
                } ?: getString(R.string.file_manager___file_details___never)
            )
            addDetail(
                label = R.string.file_manager___file_details___completed,
                value = file.prints?.success?.let { getString(R.string.x_times, it) } ?: getString(R.string.file_manager___file_details___never)
            )
            addDetail(
                label = R.string.file_manager___file_details___failures,
                value = file.prints?.failure?.let { getString(R.string.x_times, it) } ?: getString(R.string.file_manager___file_details___never)
            )

            // Make all labels same width
            val labelWidth = labels.map {
                it.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )

                it.measuredWidth
            }.maxOrNull() ?: ViewGroup.LayoutParams.WRAP_CONTENT
            labels.forEach {
                it.updateLayoutParams<LinearLayout.LayoutParams> {
                    width = labelWidth
                }
            }
        }
    }

    private fun addTitle(@StringRes title: Int) {
        binding.generatedContent.addView(
            createTextView(
                text = getString(title),
                textAppearance = R.style.OctoTheme_TextAppearance_SectionHeader,
                textColor = R.color.dark_text
            ).also {
                it.updatePadding(top = margin3)
            }
        )
    }

    private fun addDetail(@StringRes label: Int, value: CharSequence?) {
        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.updatePadding(top = margin0)
        row.addView(
            createTextView(
                text = getString(label),
                textAppearance = R.style.OctoTheme_TextAppearance_Label,
                textColor = R.color.normal_text
            ).also {
                labels.add(it)
                it.updatePadding(right = margin2)
            }
        )
        row.addView(
            createTextView(
                text = value ?: getString(R.string.unknown),
                textAppearance = R.style.OctoTheme_TextAppearance_Label,
                textColor = R.color.light_text
            )
        )
        binding.generatedContent.addView(row)
    }

    private fun createTextView(text: CharSequence, @StyleRes textAppearance: Int, @ColorRes textColor: Int): TextView {
        val view = TextView(requireContext())
        TextViewCompat.setTextAppearance(view, textAppearance)
        view.setTextColor(ContextCompat.getColor(requireContext(), textColor))
        view.text = text
        return view
    }
}