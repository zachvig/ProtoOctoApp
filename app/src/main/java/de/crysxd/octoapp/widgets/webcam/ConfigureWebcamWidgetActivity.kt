package de.crysxd.octoapp.widgets.webcam

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.widgets.AppWidgetPreferences
import de.crysxd.octoapp.widgets.webcam.BaseWebcamAppWidget.Companion.updateAppWidget
import timber.log.Timber

class ConfigureWebcamWidgetActivity : Activity() {
    private val appWidgetId
        get() = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        Timber.i("Starting ConfigureWidgetActivity for widget $appWidgetId")
        overridePendingTransition(0, 0)

        val instances = Injector.get().octorPrintRepository().getAll()
        val titles = instances.map { it.settings?.appearance?.name ?: it.webUrl.replace("https://", "").replace("http://", "") }.map { "Always $it" }
        val webUrls = instances.map { it.webUrl }

        val allTitles = listOf(listOf("Synced with the app"), titles).flatten()
        val allWebUrls = listOf(listOf(null), webUrls).flatten()

        MaterialAlertDialogBuilder(this)
            .setTitle("Which OctoPrint should this widget use?")
            .setItems(allTitles.toTypedArray()) { _, selected ->
                AppWidgetPreferences.setInstanceForWidgetId(appWidgetId, allWebUrls[selected])

                // It is the responsibility of the configuration activity to update the app widget
                updateAppWidget(this, appWidgetId)
            }
            .setOnDismissListener {
                Handler().postDelayed({
                    Timber.i("Closing ConfigureWidgetActivity")

                    // Close Activity
                    val resultValue = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    setResult(RESULT_OK, resultValue)
                    finish()

                    // Remove exit animation, dialog is moving out already
                    overridePendingTransition(0, 0)
                }, 300)
            }
            .show()
    }
}