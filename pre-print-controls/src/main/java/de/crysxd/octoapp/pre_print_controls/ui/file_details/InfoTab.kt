package de.crysxd.octoapp.pre_print_controls.ui.file_details

import android.os.Bundle
import android.transition.TransitionManager
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
import de.crysxd.octoapp.base.ext.asStyleFileSize
import de.crysxd.octoapp.octoprint.models.files.FileObject
import de.crysxd.octoapp.pre_print_controls.R
import de.crysxd.octoapp.pre_print_controls.di.Injector
import de.crysxd.octoapp.pre_print_controls.di.injectParentViewModel
import de.crysxd.octoapp.pre_print_controls.ui.CropAlphaTransformation
import kotlinx.android.synthetic.main.fragment_info_tab.*
import java.text.DateFormat
import java.util.*
import kotlin.math.roundToInt

class InfoTab : Fragment(R.layout.fragment_info_tab) {

    private val labels = mutableListOf<View>()
    private val viewModel: FileDetailsViewModel by injectParentViewModel(Injector.get().viewModelFactory())
    private val margin0 by lazy { requireContext().resources.getDimensionPixelSize(R.dimen.margin_0) }
    private val margin2 by lazy { requireContext().resources.getDimensionPixelSize(R.dimen.margin_2) }
    private val margin3 by lazy { requireContext().resources.getDimensionPixelSize(R.dimen.margin_3) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            val file = viewModel.file
            val formatDurationUseCase = de.crysxd.octoapp.base.di.Injector.get().formatDurationUseCase()

            // Load preview image
            val start = System.currentTimeMillis()
            file.thumbnail?.let {
                Injector.get().picasso().observe(viewLifecycleOwner) { picasso ->
                    picasso.load(it)
                        .noFade()
                        .transform(CropAlphaTransformation())
                        .into(preview, object : Callback {
                            override fun onError(e: Exception?) = Unit
                            override fun onSuccess() {
                                preview.post {
                                    if (start - System.currentTimeMillis() > 30) {
                                        TransitionManager.beginDelayedTransition(view as ViewGroup)
                                    }
                                    preview.isVisible = true

                                    // Limit to 16:9 at most
                                    preview.measure(
                                        View.MeasureSpec.makeMeasureSpec(generatedContent.width, View.MeasureSpec.EXACTLY),
                                        View.MeasureSpec.makeMeasureSpec((generatedContent.width * (9 / 16f)).roundToInt(), View.MeasureSpec.AT_MOST),
                                    )
                                    preview.updateLayoutParams {
                                        height = preview.measuredHeight
                                    }
                                }
                            }
                        })
                }
            }

            // Bind data
            printName.text = file.name
            addTitle(R.string.print_info)
            addDetail(
                label = R.string.print_time,
                value = file.gcodeAnalysis?.estimatedPrintTime?.let { formatDurationUseCase.execute(it) }
            )
            addDetail(
                label = R.string.model_size,
                value = file.gcodeAnalysis?.dimensions?.let { String.format(Locale.getDefault(), "%.1f × %.1f × %.1f mm", it.width, it.depth, it.height) }
            )
            addDetail(
                label = R.string.filament_use,
                value = file.gcodeAnalysis?.filament?.let {
                    val totalLength = listOfNotNull(it.tool0, it.tool1).sumByDouble { s -> s.length }
                    val totalVolume = listOfNotNull(it.tool0, it.tool1).sumByDouble { s -> s.volume }
                    String.format(Locale.getDefault(), "%.02f m / %.02f cm³", totalLength / 1000, totalVolume)
                }
            )

            addTitle(R.string.file)
            addDetail(
                label = R.string.location,
                value = when (file.origin) {
                    FileObject.FILE_ORIGIN_SD -> getString(R.string.file_location_sd_card)
                    FileObject.FILE_ORIGIN_LOCAL -> getString(R.string.file_location_local)
                    else -> getString(R.string.file_location_unknown)
                }
            )
            addDetail(
                label = R.string.path,
                value = "/" + file.path.removeSuffix(file.name).removeSuffix("/")
            )
            addDetail(
                label = R.string.size,
                value = file.size.asStyleFileSize()
            )
            addDetail(
                label = R.string.uploaded,
                value = formatDate(file.date)
            )

            addTitle(R.string.history)
            addDetail(
                label = R.string.last_print,
                value = file.prints?.last?.let {
                    getString(
                        if (it.success) {
                            R.string.last_print_at_x_success
                        } else {
                            R.string.last_print_at_x_failure
                        }, formatDate(it.date)
                    )
                } ?: getString(R.string.never)
            )
            addDetail(
                label = R.string.completed,
                value = file.prints?.success?.let { getString(R.string.x_times, it) } ?: getString(R.string.never)
            )
            addDetail(
                label = R.string.failures,
                value = file.prints?.failure?.let { getString(R.string.x_times, it) } ?: getString(R.string.never)
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

    private fun formatDate(time: Long) =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(time * 1000))

    private fun addTitle(@StringRes title: Int) {
        generatedContent.addView(
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
        generatedContent.addView(row)
    }

    private fun createTextView(text: CharSequence, @StyleRes textAppearance: Int, @ColorRes textColor: Int): TextView {
        val view = TextView(requireContext())
        TextViewCompat.setTextAppearance(view, textAppearance)
        view.setTextColor(ContextCompat.getColor(requireContext(), textColor))
        view.text = text
        return view
    }
}