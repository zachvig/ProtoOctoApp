package de.crysxd.octoapp.base

import android.net.Uri
import androidx.annotation.StringRes
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.urlEncode

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

    fun getWebcamUri(): Uri =
        getUri(R.string.uri___webcam)

    fun getTroubleShootUri(baseUrl: Uri, apiKey: String? = null): Uri =
        getUri(R.string.uri___troubleshoot, "{baseUrl}", baseUrl.toString().urlEncode(), "{apiKey}", apiKey ?: "")

    fun getFaqUri(faqId: String) =
        getUri(R.string.uri___faq, "{faqId}", faqId)

    fun isActiveInstanceRequired(uri: Uri) = when (uri.path) {
        getConfigureRemoteAccessUri().path -> true
        else -> false
    }
}