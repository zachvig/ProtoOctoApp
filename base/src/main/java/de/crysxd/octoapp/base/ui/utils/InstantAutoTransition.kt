package de.crysxd.octoapp.base.ui.utils

import android.graphics.Rect
import androidx.transition.*
import com.transitionseverywhere.ChangeText

class InstantAutoTransition(
    explode: Boolean = false,
    quickTransition: Boolean = false,
    explodeEpicenter: Rect? = null,
) : TransitionSet() {

    init {
        duration = if (quickTransition) 150 else 300

        addTransition(ChangeBounds())
        addTransition(Fade())
        addTransition(ChangeTransform())
        addTransition(ChangeScroll())
        addTransition(ChangeImageTransform())
        addTransition(ChangeClipBounds())
        addTransition(ChangeTextSizeTransform())
        addTransition(ChangeText().apply {
            changeBehavior = ChangeText.CHANGE_BEHAVIOR_OUT_IN
            duration = this@InstantAutoTransition.duration / 3
        })

        if (explode) {
            addTransition(Explode().apply {
                explodeEpicenter?.let {
                    epicenterCallback = object : EpicenterCallback() {
                        override fun onGetEpicenter(transition: Transition) = it
                    }
                }
            })
        }


    }
}