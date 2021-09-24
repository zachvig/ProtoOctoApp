package de.crysxd.octoapp.base

import android.net.Uri
import android.util.Base64
import androidx.annotation.StringRes
import de.crysxd.octoapp.base.di.Injector
import okhttp3.HttpUrl

object UriLibrary {
    private fun getUri(@StringRes string: Int, vararg placeholder: String) =
        Uri.parse("http://" + Injector.get().context().getString(string).let {
            var out = it
            for (i in placeholder.indices step 2) {
                out = out.replace(placeholder[i], placeholder[i + 1])
            }
            out
        })

    fun getConfigureRemoteAccessUri(): Uri =
        getUri(R.string.uri___configure_remote_access)

    fun getHelpUri(): Uri =
        getUri(R.string.uri___help)

    fun getTutorialsUri(): Uri =
        getUri(R.string.uri___help)

    fun getWebcamTroubleshootingUri(): Uri =
        getUri(R.string.uri___webcam_troubleshooting)

    fun getFileManagerUri(): Uri =
        getUri(R.string.uri___file_manager)

    fun getWebcamUri(): Uri =
        getUri(R.string.uri___webcam)

    fun getFixOctoPrintConnectionUri(baseUrl: HttpUrl, instanceId: String): Uri =
        getUri(
            R.string.uri___fix_octoprint_connection,
            "{baseUrl}",
            secureEncode(baseUrl.toString()),
            "{instanceId}",
            instanceId
        )

    fun getFaqUri(faqId: String) =
        getUri(R.string.uri___faq, "{faqId}", faqId)

    fun getPurchaseUri(): Uri =
        getUri(R.string.uri___purchase)

    fun getCompanionPluginUri(): Uri = Uri.parse("https://plugins.octoprint.org")

    fun isActiveInstanceRequired(uri: Uri) = when (uri.path) {
        getConfigureRemoteAccessUri().path -> true
        else -> false
    }

    // When passing URLs as query params, weird shit is happening where Android would "double decode" URLs in params causing
    // problems if the original URL contains encoded parts. To circumvent this we encode as Base64
    fun secureEncode(value: String): String = Base64.encodeToString(value.toByteArray(), Base64.NO_WRAP)
    fun secureDecode(value: String) = String(Base64.decode(value, Base64.NO_WRAP))
}