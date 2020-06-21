package de.crysxd.octoapp.base.ui.utils

import androidx.transition.*

class InstantAutoTransition(
    explode: Boolean = false,
    quickTransition: Boolean = false
) : TransitionSet() {

    init {
        addTransition(ChangeBounds())
        addTransition(Fade())
        addTransition(ChangeTransform())
        addTransition(ChangeScroll())
        addTransition(ChangeImageTransform())
        addTransition(ChangeClipBounds())
        addTransition(ChangeTextSizeTransform())

        if (explode) {
            addTransition(Explode())
        }

        if (quickTransition) {
            duration = 150
        }
    }
}