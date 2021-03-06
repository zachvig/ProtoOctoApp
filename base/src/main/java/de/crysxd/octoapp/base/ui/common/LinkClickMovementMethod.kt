package de.crysxd.octoapp.base.ui.common

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.MotionEvent
import android.widget.TextView
import de.crysxd.octoapp.base.ext.open

/**
 * Set this on a textview and then you can potentially open links locally if applicable
 */
class LinkClickMovementMethod(private val mOnLinkClickedListener: OnLinkClickedListener) : LinkMovementMethod() {
    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val action = event.action

        //http://stackoverflow.com/questions/1697084/handle-textview-link-click-in-my-android-app
        if (action == MotionEvent.ACTION_UP) {
            var x = event.x.toInt()
            var y = event.y.toInt()
            x -= widget.totalPaddingLeft
            y -= widget.totalPaddingTop
            x += widget.scrollX
            y += widget.scrollY
            val layout = widget.layout
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())
            val link = buffer.getSpans(off, off, URLSpan::class.java)
            if (link.size != 0) {
                val url = link[0].url
                val handled = mOnLinkClickedListener.onLinkClicked(widget.context, url)
                return if (handled) {
                    true
                } else {
                    super.onTouchEvent(widget, buffer, event)
                }
            }
        }
        return super.onTouchEvent(widget, buffer, event)
    }

    fun interface OnLinkClickedListener {
        fun onLinkClicked(context: Context, url: String?): Boolean
    }

    open class OpenWithIntentLinkClickedListener(private val activity: Activity) : OnLinkClickedListener {
        override fun onLinkClicked(context: Context, url: String?): Boolean {
            Uri.parse(url).open(activity)
            return true
        }
    }
}