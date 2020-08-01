package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.content.FileProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.Injector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject


class OpenEmailClientForFeedbackUseCase @Inject constructor() : UseCase<OpenEmailClientForFeedbackUseCase.Params, Unit> {

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun execute(param: Params) = withContext(Dispatchers.IO) {
        val context = param.context

        val email = Firebase.remoteConfig.getString("contact_email")
        val (version, versionCode) = context.packageManager.getPackageInfo(context.packageName, 0).let {
            @Suppress("DEPRECATION")
            Pair(it.versionName, it.versionCode)
        }
        val subject = "Feedback OctoApp $version"

        val publicDir = File(context.externalCacheDir, context.getString(R.string.public_file_dir_name))
        publicDir.mkdir()

        val zipFile = File(publicDir, "data.zip")
        val zipStream = ZipOutputStream(zipFile.outputStream())
        val zipFileUri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", zipFile)
        var fileCount = 0

        if (param.sendLogs) {
            zipStream.putNextEntry(ZipEntry("logs.log"))
            zipStream.writer().apply {
                write(Injector.get().timberCacheTree().logs)
                flush()
            }
            zipStream.closeEntry()
            fileCount++
        }

        if (param.sendPhoneInfo) {
            zipStream.putNextEntry(ZipEntry("phone_info.json"))
            val build = JsonObject()
            build.addProperty("brand", Build.BRAND)
            build.addProperty("manufacturer", Build.MANUFACTURER)
            build.addProperty("model", Build.MODEL)
            build.addProperty("product", Build.PRODUCT)
            build.addProperty("display", Build.DISPLAY)
            build.addProperty("supported_abis", Build.SUPPORTED_ABIS.joinToString())
            build.addProperty("supported_abis_32_bit", Build.SUPPORTED_32_BIT_ABIS.joinToString())
            build.addProperty("supported_abis_64_bit", Build.SUPPORTED_64_BIT_ABIS.joinToString())
            val buildVersion = JsonObject()
            build.addProperty("sdk", Build.VERSION.SDK_INT)
            build.addProperty("release", Build.VERSION.RELEASE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                build.addProperty("preview_sdk", Build.VERSION.PREVIEW_SDK_INT)
                build.addProperty("security_patch", Build.VERSION.SECURITY_PATCH)
            }
            build.add("version", buildVersion)
            val appVersion = JsonObject()
            appVersion.addProperty("version_name", version)
            appVersion.addProperty("version_code", versionCode)
            val others = JsonObject()
            appVersion.addProperty("language", Locale.getDefault().isO3Language)
            appVersion.addProperty("country", Locale.getDefault().isO3Country)
            val phoneInfo = JsonObject()
            phoneInfo.add("build", build)
            phoneInfo.add("app", appVersion)
            phoneInfo.add("others", others)

            zipStream.writer().apply {
                write(phoneInfo.toString())
                flush()
            }
            zipStream.closeEntry()
            fileCount++
        }

        if (param.sendOctoPrintInfo) {
            zipStream.putNextEntry(ZipEntry("octoprint_info.json"))
            zipStream.writer().apply {
                write(Gson().toJson(Injector.get().octorPrintRepository().instanceInformation.value?.copy(apiKey = "***")))
                flush()
            }
            zipStream.closeEntry()
            fileCount++
        }

        param.screenshot?.let {
            zipStream.putNextEntry(ZipEntry("screenshot.webp"))
            it.compress(Bitmap.CompressFormat.WEBP, 75, zipStream)
            zipStream.closeEntry()
            fileCount++
        }

        zipStream.close()

        val intent = Intent(Intent.ACTION_SEND).also {
            it.type = "message/rfc822"
            it.putExtra(Intent.EXTRA_SUBJECT, subject)
            it.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))

            if (fileCount > 0) {
                it.putExtra(Intent.EXTRA_STREAM, zipFileUri)
            }
        }

        context.startActivity(intent)
    }

    data class Params(
        val context: Context,
        val sendPhoneInfo: Boolean,
        val sendLogs: Boolean,
        val sendOctoPrintInfo: Boolean,
        val screenshot: Bitmap?
    )
}