package de.crysxd.octoapp.base.ext

import android.content.Intent
import android.net.Uri
import de.crysxd.octoapp.base.ui.base.OctoActivity
import timber.log.Timber


fun Uri.open(octoActivity: OctoActivity) {
    try {
        when {
            this.host == "app.octoapp.eu" -> {
                Timber.i("Opening in-app link: $this")
                octoActivity.navController.navigate(this)
            }

            else -> {
                Timber.i("Opening external link: $this")

                val intent = Intent(Intent.ACTION_VIEW, this).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                octoActivity.startActivity(intent)
            }
        }
    } catch (e: Exception) {
        Timber.e(e)
        octoActivity.showDialog("This content is currently unavailable, try again later!")
    }
}