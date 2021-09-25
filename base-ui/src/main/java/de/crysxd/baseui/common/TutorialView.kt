package de.crysxd.baseui.common

import android.content.Context
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.edit
import androidx.core.content.res.use
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import de.crysxd.baseui.databinding.TutorialViewBinding
import de.crysxd.baseui.R
import de.crysxd.octoapp.base.di.BaseInjector


class TutorialView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleRes: Int = 0) : FrameLayout(
    context,
    attributeSet,
    defStyleRes
) {

    var onHideAction = {}
    var onLearnMoreAction = {}
    private var sharedPrefKey: String? = null
    private var isSharedPrefInverted = false

    companion object {
        fun isTutorialVisible(prefKey: String, isInvertedLogic: Boolean = false): Boolean {
            val value = BaseInjector.get().sharedPreferences().getBoolean(prefKey, isInvertedLogic)
            return if (isInvertedLogic) value else !value
        }
    }

    init {
        val prefs = if (isInEditMode) null else BaseInjector.get().sharedPreferences()

        context.obtainStyledAttributes(attributeSet, R.styleable.TutorialView).use {
            sharedPrefKey = if (isInEditMode) null else it.getString(R.styleable.TutorialView_hidePrefName)
            isSharedPrefInverted = it.getBoolean(R.styleable.TutorialView_hidePrefInverted, false)

            val alwaysShown = it.getBoolean(R.styleable.TutorialView_alwaysShow, false)
            val visible = sharedPrefKey?.let { key -> isTutorialVisible(key, isSharedPrefInverted) } ?: false
            val animations = it.getBoolean(R.styleable.TutorialView_animations, true)
            isVisible = visible || isInEditMode || alwaysShown

            // Only inflate view if visible
            if (isVisible) {
                val binding = TutorialViewBinding.inflate(LayoutInflater.from(context), this, true)

                if (it.getBoolean(R.styleable.TutorialView_noBottomMargin, false)) {
                    binding.root.updateLayoutParams<LayoutParams> { bottomMargin = 0 }
                }

                binding.hero.setImageDrawable(it.getDrawable(R.styleable.TutorialView_hero))
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
                    sharedPrefKey?.let {
                        prefs?.edit { putBoolean(sharedPrefKey, !isSharedPrefInverted) }
                    }

                    onHideAction()

                    // If always shown is set somebody else takes care of hiding the view
                    if (!alwaysShown) {
                        if (animations) {
                            TransitionManager.beginDelayedTransition(rootView as ViewGroup)
                        }
                        isVisible = false
                    }
                }
            }
        }
    }
}