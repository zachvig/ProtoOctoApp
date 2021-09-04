package de.crysxd.octoapp.framework

import android.net.Uri
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.resolve
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

object VirtualPrinterUtils {

    fun OctoPrintInstanceInformationV2.setVirtualPrinterEnabled(enabled: Boolean) {
        Timber.i("Setting virtual printer enabled=$enabled")
        val request = Request.Builder()
            .url(Uri.parse(webUrl).buildUpon().resolve("api/settings").build().toString())
            .post("{  \"plugins\": {   \"virtual_printer\": {     \"enabled\": $enabled } }}".toRequestBody("application/json".toMediaType()))
            .build()
        Injector.get().octoPrintProvider().createAdHocOctoPrint(this).createOkHttpClient().newCall(request).execute()
    }
}