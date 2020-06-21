package de.crysxd.octoapp.base.ui.utils

import android.animation.Animator
import android.animation.ObjectAnimator
import android.util.Property
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView
import androidx.transition.Transition
import androidx.transition.TransitionValues

/**
 * From  https://stackoverflow.com/a/26813670/3763626
 */
class ChangeTextSizeTransform : Transition() {

    override fun captureStartValues(transitionValues: TransitionValues) = captureValues(transitionValues)

    override fun captureEndValues(transitionValues: TransitionValues) = captureValues(transitionValues)

    private fun captureValues(transitionValues: TransitionValues) {
        if (transitionValues.view is TextView) {
            val textView = transitionValues.view as TextView
            transitionValues.values[PROPNAME_TEXT_SIZE] = textView.textSize
        }
    }

    override fun createAnimator(sceneRoot: ViewGroup, startValues: TransitionValues?, endValues: TransitionValues?): Animator? {
        val startSize = startValues?.values?.get(PROPNAME_TEXT_SIZE) as Float?
        val endSize = endValues?.values?.get(PROPNAME_TEXT_SIZE) as Float?
        if (startSize == null || endSize == null || startSize == endSize) {
            return null
        }
        val view = endValues!!.view as TextView
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, startSize)
        return ObjectAnimator.ofFloat<TextView?>(view, TEXT_SIZE_PROPERTY, startSize, endSize)
    }

    companion object {
        private val PROPNAME_TEXT_SIZE: String? = "alexjlockwood:transition:textsize"

        private val TEXT_SIZE_PROPERTY: Property<TextView?, Float>? = object : Property<TextView?, Float>(Float::class.java, "textSize") {
            override operator fun get(textView: TextView?): Float? {
                return textView!!.textSize
            }

            override operator fun set(textView: TextView?, textSizePixels: Float?) {
                textView!!.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePixels!!)
            }
        }
    }
}