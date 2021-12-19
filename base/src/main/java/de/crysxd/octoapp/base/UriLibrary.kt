package de.crysxd.octoapp.base

import android.net.Uri
import android.util.Base64
import androidx.annotation.StringRes
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.di.BaseInjector
import okhttp3.HttpUrl
import timber.log.Timber

object UriLibrary {
    private fun getUri(@StringRes string: Int, vararg placeholder: String) =
        Uri.parse("http://" + BaseInjector.get().context().getString(string).let {
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

    fun getTimelapseArchiveUri(): Uri =
        getUri(R.string.uri___timelapse_archive)

    fun getTutorialsUri(): Uri =
        getUri(R.string.uri___tutorials)

    fun getWebcamTroubleshootingUri(): Uri =
        getUri(R.string.uri___webcam_troubleshooting)

    fun getFileManagerUri(): Uri =
        getUri(R.string.uri___file_manager)

    fun getWebcamUri(): Uri =
        getUri(R.string.uri___webcam)

    fun getPluginLibraryUri(category: String? = null): Uri =
        getUri(R.string.uri___plugin_library, "{category}", category ?: "")

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

    fun getCompanionPluginUri(): Uri = Uri.parse(Firebase.remoteConfig.getString("companion_plugin_url"))

    fun isActiveInstanceRequired(uri: Uri) = when (uri.path) {
        getConfigureRemoteAccessUri().path -> true
        else -> false
    }

    // When passing URLs as query params, weird shit is happening where Android would "double decode" URLs in params causing
    // problems if the original URL contains encoded parts. To circumvent this we encode as Base64
    fun secureEncode(value: String): String = Base64.encodeToString(value.toByteArray(), Base64.NO_WRAP)
    fun secureDecode(value: String): String {
        val decoded = String(Base64.decode(value, Base64.NO_WRAP))
        Timber.i("Decoding $decoded")
        return decoded
    }
}