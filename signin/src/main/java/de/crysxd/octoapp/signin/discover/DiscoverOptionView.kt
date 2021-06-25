package de.crysxd.octoapp.signin.discover

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import de.crysxd.octoapp.octoprint.extractAndRemoveUserInfo
import de.crysxd.octoapp.signin.R
import de.crysxd.octoapp.signin.databinding.DiscoverOptionBinding

class DiscoverOptionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleRes: Int = 0
) : LinearLayoutCompat(
    context, attrs, defStyleRes
) {
    private val binding = DiscoverOptionBinding.inflate(LayoutInflater.from(context), this)
    var isDeleteVisible: Boolean
        get() = binding.buttonDelete.isVisible
        set(value) {
            binding.buttonDelete.isVisible = value
        }
    var onDelete: () -> Unit = {}
        set(value) {
            field = value
            binding.buttonDelete.setOnClickListener { value() }
        }
    private var optionId: String = ""

    init {
        gravity = Gravity.CENTER_VERTICAL
    }

    override fun setOnClickListener(l: OnClickListener?) {
        binding.card.setOnClickListener(l)
    }

    fun show(option: DiscoverOctoPrintUseCase.DiscoveredOctoPrint) {
        optionId = option.webUrl
        binding.title.text = option.label
        binding.subtitle.text = option.detailLabel
        binding.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_style_printer_background))
        binding.buttonDelete.setColorFilter(ContextCompat.getColor(context, R.color.menu_style_printer_foreground))
        binding.shevron.setColorFilter(ContextCompat.getColor(context, R.color.menu_style_printer_foreground))
    }

    fun isShowing(option: DiscoverOctoPrintUseCase.DiscoveredOctoPrint) = option.webUrl == optionId

    fun show(option: OctoPrintInstanceInformationV2, enabled: Boolean) {
        optionId = option.webUrl
        alpha = if (enabled) 1f else 0.2f
        binding.shevron.isVisible = enabled
        binding.title.text = option.label
        binding.subtitle.text = option.webUrl.extractAndRemoveUserInfo().first.takeIf { option.label != option.webUrl }
        binding.subtitle.isVisible = binding.subtitle.text.isNotBlank()
        binding.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_style_octoprint_background))
        binding.buttonDelete.setColorFilter(ContextCompat.getColor(context, R.color.menu_style_octoprint_foreground))
        binding.shevron.setColorFilter(ContextCompat.getColor(context, R.color.menu_style_octoprint_foreground))
    }

    fun isShowing(option: OctoPrintInstanceInformationV2) = option.webUrl == optionId

    fun showManualConnect() {
        binding.title.text = "Connect manually H"
        binding.subtitle.text = "Copy a web URL from your browser H"
        binding.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_style_settings_background))
        binding.buttonDelete.setColorFilter(ContextCompat.getColor(context, R.color.menu_style_settings_foreground))
        binding.shevron.setColorFilter(ContextCompat.getColor(context, R.color.menu_style_settings_foreground))
    }

    fun showDelete() {
        binding.buttonDelete.isVisible = true
    }

    fun showQuickSwitchOption() {
        binding.title.text = "Quick switch not enabled H"
        binding.subtitle.text = "Enable to reconnect H"
        binding.card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_style_support_background))
        binding.buttonDelete.setColorFilter(ContextCompat.getColor(context, R.color.menu_style_support_foreground))
        binding.shevron.setColorFilter(ContextCompat.getColor(context, R.color.menu_style_support_foreground))
    }
}