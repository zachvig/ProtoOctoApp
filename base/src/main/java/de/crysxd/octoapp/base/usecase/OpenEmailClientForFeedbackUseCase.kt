package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject


class OpenEmailClientForFeedbackUseCase @Inject constructor() : UseCase<OpenEmailClientForFeedbackUseCase.Params, Unit> {

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun execute(param: Params) = withContext(Dispatchers.IO) {
        val context = param.context

        val email = Firebase.remoteConfig.getString("contact_email")
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        val subject = "Feedback OctoApp $version"

        val publicDir = File(context.externalCacheDir, context.getString(R.string.public_file_dir_name))
        publicDir.mkdir()

        val zipFile = File(publicDir, "data.zip")
        val zipStream = ZipOutputStream(zipFile.outputStream())
        val zipFileUri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", zipFile)
        var fileCount = 0

        param.logs?.let {
            zipStream.putNextEntry(ZipEntry("logs.txt"))
            zipStream.writer().write(it)
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

    data class Params(val context: Context, val logs: String?, val screenshot: Bitmap?)
}