package de.crysxd.octoapp.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.widgets.webcam.updateAppWidget
import timber.log.Timber

class ConfigureWidgetActivity : Activity() {
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
        val titles = instances.map { it.settings?.appearance?.name ?: it.webUrl.replace("https://", "").replace("http://", "") }
        val webUrls = instances.map { it.webUrl }

        val allTitles = listOf(listOf("Always active instance"), titles).flatten()
        val allWebUrls = listOf(listOf(null), webUrls).flatten()

        MaterialAlertDialogBuilder(this)
            .setTitle("Select which OctoPrint is displayed by this widget")
            .setItems(allTitles.toTypedArray()) { _, selected ->
                WidgetPreferences.setInstanceForWidgetId(appWidgetId, allWebUrls[selected])

                // It is the responsibility of the configuration activity to update the app widget
                val appWidgetManager = AppWidgetManager.getInstance(this)
                updateAppWidget(this, appWidgetManager, appWidgetId)
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