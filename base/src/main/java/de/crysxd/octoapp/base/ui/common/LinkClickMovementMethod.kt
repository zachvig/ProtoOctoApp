package de.crysxd.octoapp.base.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.MotionEvent
import android.widget.TextView
import android.widget.Toast

/**
 * Set this on a textview and then you can potentially open links locally if applicable
 */
class LinkClickMovementMethod(private val mOnLinkClickedListener: OnLinkClickedListener = OpenWithIntentLinkClickedListener()) : LinkMovementMethod() {
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

    open class OpenWithIntentLinkClickedListener : OnLinkClickedListener {
        override fun onLinkClicked(context: Context, url: String?): Boolean {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "Unable to open link, no app found", Toast.LENGTH_SHORT).show()
            }

            return true
        }
    }
}