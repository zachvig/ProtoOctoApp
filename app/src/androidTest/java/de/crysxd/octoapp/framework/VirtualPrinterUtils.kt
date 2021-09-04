package de.crysxd.octoapp.framework

import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.octoprint.resolvePath
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

object VirtualPrinterUtils {

    fun OctoPrintInstanceInformationV3.setVirtualPrinterEnabled(enabled: Boolean) {
        Timber.i("Setting virtual printer enabled=$enabled")
        val request = Request.Builder()
            .url(webUrl.resolvePath("api/settings"))
            .post("{  \"plugins\": {   \"virtual_printer\": {     \"enabled\": $enabled } }}".toRequestBody("application/json".toMediaType()))
            .build()
        Injector.get().octoPrintProvider().createAdHocOctoPrint(this).createOkHttpClient().newCall(request).execute()
    }
}