package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.edit
import androidx.core.content.res.use
import androidx.core.view.isVisible
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.databinding.TutorialViewBinding
import de.crysxd.octoapp.base.di.Injector


class TutorialView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleRes: Int = 0) : FrameLayout(
    context,
    attributeSet,
    defStyleRes
) {

    var onHideAction = {}
    var onLearnMoreAction = {}
    private var sharedPrefKey: String? = null
    private var isSharedPrefInverted = false

    init {
        val prefs = if (isInEditMode) null else Injector.get().sharedPreferences()

        context.obtainStyledAttributes(attributeSet, R.styleable.TutorialView).use {
            sharedPrefKey = it.getString(R.styleable.TutorialView_hidePrefName)
            isSharedPrefInverted = it.getBoolean(R.styleable.TutorialView_hidePrefInverted, false)

            val alwaysShown = it.getBoolean(R.styleable.TutorialView_alwaysShow, false)
            val hidden = sharedPrefKey?.let { prefs?.getBoolean(sharedPrefKey, isSharedPrefInverted) } ?: false
            isVisible = !(if (isSharedPrefInverted) !hidden else hidden) || isInEditMode || alwaysShown

            // Only inflate view if visible
            if (isVisible) {
                val binding = TutorialViewBinding.inflate(LayoutInflater.from(context), this, true)

                binding.title.text = it.getString(R.styleable.TutorialView_title)
                binding.detail.text = it.getString(R.styleable.TutorialView_detail)
                binding.detail.isVisible = binding.detail.text.isNotBlank()
                binding.buttonHideHint.text = it.getString(R.styleable.TutorialView_hideButton) ?: context.getString(R.string.hide)
                binding.buttonLearnMore.text = it.getString(R.styleable.TutorialView_learnMoreButton)
                binding.buttonLearnMore.isVisible = binding.buttonLearnMore.text.isNotBlank()
                binding.buttonLearnMore.setOnClickListener {
                    onLearnMoreAction()
                }
                binding.buttonHideHint.setOnClickListener {
                    // If always shown is set sombody else takes care of hiding the view
                    if (!alwaysShown) {
                        TransitionManager.beginDelayedTransition(rootView as ViewGroup)
                        isVisible = false
                    }

                    onHideAction()
                    sharedPrefKey?.let {
                        prefs?.edit { putBoolean(sharedPrefKey, !isSharedPrefInverted) }
                    }
                }
            }
        }
    }
}