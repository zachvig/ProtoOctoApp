package de.crysxd.octoapp.base.ext

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.navigation.NavOptions
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.ui.base.OctoActivity
import timber.log.Timber

var mainActivityClass: Class<out Activity>? = null

fun Uri.open(octoActivity: Activity, allowCustomTabs: Boolean = true) {
    Timber.i("LINK: $this")
    Timber.i("LINK: ${this.host}")
    try {
        when {
            this.host == "app.octoapp.eu" -> {
                Timber.i("Opening in-app link: $this")
                (octoActivity as? OctoActivity)?.navController?.navigate(
                    this,
                    NavOptions.Builder()
                        .setEnterAnim(R.anim.enterAnim)
                        .setExitAnim(R.anim.exitAnim)
                        .setPopEnterAnim(R.anim.popEnterAnim)
                        .setPopExitAnim(R.anim.popExitAnim)
                        .setLaunchSingleTop(true)
                        .build()
                ) ?: let {
                    val intent = Intent(octoActivity, mainActivityClass)
                    intent.data = this
                    intent.action = Intent.ACTION_VIEW
                    octoActivity.startActivity(intent)
                }
            }

            (scheme == "http" || scheme == "https") && allowCustomTabs -> {
                try {
                    Timber.i("Opening custom tab link: $this")
                    val builder = CustomTabsIntent.Builder()
                    val customTabsIntent = builder.build()
                    customTabsIntent.launchUrl(octoActivity, this)
                } catch (e: java.lang.Exception) {
                    open(octoActivity, false)
                }
            }

            else -> {
                Timber.i("Opening external link: $this")
                val intent = Intent(Intent.ACTION_VIEW, this).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                octoActivity.startActivity(intent)
            }
        }
    } catch (e: Exception) {
        Timber.e(e)
        val message = octoActivity.getString(R.string.help___content_not_available)
        (octoActivity as? OctoActivity)?.showDialog(message) ?: Toast.makeText(octoActivity, message, Toast.LENGTH_SHORT).show()
    }
}