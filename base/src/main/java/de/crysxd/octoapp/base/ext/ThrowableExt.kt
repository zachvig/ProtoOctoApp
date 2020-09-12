package de.crysxd.octoapp.base.ext

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import de.crysxd.octoapp.base.R

fun Throwable.composeGeneralErrorMessage(context: Context, @StringRes baseStringRes: Int = R.string.error_general_with_details): CharSequence = HtmlCompat.fromHtml(
    context.getString(
        baseStringRes,
        ContextCompat.getColor(context, R.color.light_text),
        localizedMessage ?: cause?.localizedMessage ?: this::class.java.simpleName
    ), HtmlCompat.FROM_HTML_MODE_LEGACY
)

fun Throwable.composeMessageStack(): CharSequence {
    val messageBuilder = StringBuilder()
    messageBuilder.append("<b>Error:</b> ")
    messageBuilder.append(localizedMessage ?: this::class.java.simpleName)

    var cause = cause
    while (cause != null) {
        messageBuilder.append("<br/><br/>")
        messageBuilder.append("<b>Caused by:</b> ")
        messageBuilder.append(cause.localizedMessage)
        cause = cause.cause
    }

    return HtmlCompat.fromHtml(messageBuilder.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
}