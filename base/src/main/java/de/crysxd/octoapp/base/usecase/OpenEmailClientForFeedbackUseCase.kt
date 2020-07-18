package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import javax.inject.Inject

class OpenEmailClientForFeedbackUseCase @Inject constructor() : UseCase<Context, Unit> {

    override suspend fun execute(param: Context) {
        val email = Firebase.remoteConfig.getString("contact_email")
        val version = param.packageManager.getPackageInfo(param.packageName, 0).versionName
        val subject = "Feedback OctoApp $version"
        val uri = Uri.parse("mailto:$email?subject=$subject")
        param.startActivity(Intent(Intent.ACTION_SENDTO, uri).also {
            it.putExtra(Intent.EXTRA_SUBJECT, subject)
        })
    }
}