package de.crysxd.octoapp.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.annotation.RequiresApi
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import de.crysxd.octoapp.EXTRA_TARGET_OCTOPRINT_WEB_URL
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.LocalizedActivity
import de.crysxd.octoapp.base.ui.colorTheme
import de.crysxd.octoapp.widgets.AppWidgetPreferences.ACTIVE_WEB_URL_MARKER
import timber.log.Timber
import java.util.*

class ConfigureAppWidgetActivity : LocalizedActivity() {
    private val appWidgetId
        get() = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        Timber.i("Starting ConfigureWidgetActivity for widget $appWidgetId")
        overridePendingTransition(0, 0)

        val maxWidgetCount = Firebase.remoteConfig.getLong("number_of_free_app_widgets")
        if (getWidgetCount(this) > maxWidgetCount || BillingManager.isFeatureEnabled("infinite_app_widgets")) {
            MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.app_widget___free_widgets_exceeded_message, maxWidgetCount))
                .setPositiveButton(R.string.app_widget___free_widgets_exceeded_action, null)
                .setOnDismissListener { finish() }
                .show()
        } else when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_CONFIGURE -> configureAppWidget()
            Intent.ACTION_CREATE_SHORTCUT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                configureShortcut()
            } else {
                finish()
            }
            else -> {
                Timber.e(IllegalArgumentException("Invalid action ${intent.action}"))
                finish()
            }
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun configureShortcut() {
        showSelectionDialog { webUrl ->
            if (webUrl == null) return@showSelectionDialog null

            ShortcutManager::class.java
            val instance = Injector.get().octorPrintRepository().getAll().firstOrNull { it.webUrl == webUrl }
            val label = instance?.label ?: getString(R.string.app_name)
            val drawable = when (instance.colorTheme.colorRes) {
                R.color.blue_color_scheme -> R.mipmap.ic_launcher_blue
                R.color.yellow_color_scheme -> R.mipmap.ic_launcher_yellow
                R.color.red_color_scheme -> R.mipmap.ic_launcher_red
                R.color.green_color_scheme -> R.mipmap.ic_launcher_green
                R.color.violet_color_scheme -> R.mipmap.ic_launcher_violet
                R.color.orange_color_scheme -> R.mipmap.ic_launcher_orange
                R.color.white_color_scheme -> R.mipmap.ic_launcher_white
                R.color.black_color_scheme -> R.mipmap.ic_launcher_black
                else -> R.mipmap.ic_launcher
            }
            val icon = Icon.createWithResource(this, drawable)
            val manager = getSystemService(ShortcutManager::class.java)
            val info = ShortcutInfo.Builder(this, UUID.randomUUID().toString())
                .setIntent(Intent(this, MainActivity::class.java).also {
                    it.action = Intent.ACTION_VIEW
                    it.putExtra(EXTRA_TARGET_OCTOPRINT_WEB_URL, webUrl)
                })
                .setShortLabel(label)
                .setIcon(icon)
                .build()

            manager.createShortcutResultIntent(info)
        }
    }

    private fun configureAppWidget() {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        showSelectionDialog {
            // It is the responsibility of the configuration activity to update the app widget
            AppWidgetPreferences.setInstanceForWidgetId(appWidgetId, it)
            updateAppWidget(appWidgetId)

            // Create result intnet
            Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
        }
    }

    private fun showSelectionDialog(result: (webUrl: String?) -> Intent?) {
        val instances = Injector.get().octorPrintRepository().getAll()
        val titles = instances.map { it.label }.map { getString(R.string.app_widget___link_widget__option_x, it) }
        val webUrls = instances.map { it.webUrl }

        val allTitles = listOf(listOf(getString(R.string.app_widget___link_widget__option_synced)), titles).flatten()
        val allWebUrls = listOf(listOf(ACTIVE_WEB_URL_MARKER), webUrls).flatten()
        var selectedUrl: String? = null
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.app_widget___link_widget_title)
            .setItems(allTitles.toTypedArray()) { _, selected ->
                selectedUrl = allWebUrls[selected]

            }
            .setOnDismissListener {
                Handler().postDelayed({
                    Timber.i("Closing ConfigureWidgetActivity")

                    setResult(RESULT_OK, result(selectedUrl))
                    finish()
                }, 300)
            }
            .show()
    }
}