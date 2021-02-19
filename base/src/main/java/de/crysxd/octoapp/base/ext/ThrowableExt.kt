package de.crysxd.octoapp.base.ext

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.models.exceptions.UserMessageException
import de.crysxd.octoapp.octoprint.exceptions.OctoPrintException
import de.crysxd.octoapp.octoprint.exceptions.ProxyException

fun Throwable.composeErrorMessage(context: Context, @StringRes baseStringRes: Int = R.string.error_general_with_details): CharSequence = HtmlCompat.fromHtml(
    (this as? UserMessageException)?.userMessage?.let { context.getString(it) }?.htmlify()
        ?: (this as? OctoPrintException)?.userFacingMessage?.htmlify()
        ?: context.getString(
            baseStringRes,
            ContextCompat.getColor(context, R.color.light_text),
            (this as? ProxyException)?.originalMessage?.htmlify() ?: localizedMessage?.htmlify()
            ?: cause?.localizedMessage?.htmlify()
            ?: this::class.java.simpleName
        ), HtmlCompat.FROM_HTML_MODE_LEGACY
)

fun Throwable.composeMessageStack(): CharSequence {
    val messageBuilder = StringBuilder()
    messageBuilder.append("<b>Error:</b> ")
    messageBuilder.append((this as? ProxyException)?.originalMessage ?: localizedMessage ?: this::class.java.simpleName)

    var cause = cause
    while (cause != null) {
        messageBuilder.append("<br/><br/>")
        messageBuilder.append("<b>Caused by:</b> ")
        messageBuilder.append(((this as? ProxyException)?.originalMessage ?: cause.localizedMessage)?.htmlify())
        cause = cause.cause
    }

    return HtmlCompat.fromHtml(messageBuilder.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
}

private fun CharSequence.htmlify() = this.replace(Regex("\n"), "<br/>")