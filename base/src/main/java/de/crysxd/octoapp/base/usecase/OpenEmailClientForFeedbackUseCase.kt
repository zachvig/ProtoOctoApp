package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.logging.TimberCacheTree
import java.io.File
import javax.inject.Inject

class OpenEmailClientForFeedbackUseCase @Inject constructor(private val cacheTree: TimberCacheTree) : UseCase<Context, Unit> {

    override suspend fun execute(param: Context) {
        val email = Firebase.remoteConfig.getString("contact_email")
        val version = param.packageManager.getPackageInfo(param.packageName, 0).versionName
        val subject = "Feedback OctoApp $version"

        val publicDir = File(param.externalCacheDir, param.getString(R.string.public_file_dir_name))
        publicDir.mkdir()
        val file = File(publicDir, "logs.txt")
        file.writeText(cacheTree.logs)
        val fileUri = FileProvider.getUriForFile(param, param.applicationContext.packageName + ".provider", file)

        val intent = Intent(Intent.ACTION_SEND).also {
            it.type = "message/rfc822"
            it.putExtra(Intent.EXTRA_SUBJECT, subject)
            it.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            it.putExtra(Intent.EXTRA_STREAM, fileUri)
        }

        param.startActivity(intent)
    }
}