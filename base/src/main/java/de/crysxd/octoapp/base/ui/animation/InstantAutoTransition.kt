package de.crysxd.octoapp.base.ui.animation

import androidx.transition.*

class InstantAutoTransition : TransitionSet() {

    init {
        addTransition(ChangeBounds())
        addTransition(Fade())
        addTransition(ChangeTransform())
        addTransition(ChangeScroll())
        addTransition(ChangeImageTransform())
        addTransition(ChangeClipBounds())
    }
}