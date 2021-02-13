package de.crysxd.octoapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.usecase.CancelPrintJobUseCase
import de.crysxd.octoapp.widgets.progress.ProgressAppWidget
import de.crysxd.octoapp.widgets.webcam.BaseWebcamAppWidget
import de.crysxd.octoapp.widgets.webcam.ControlsWebcamAppWidget
import de.crysxd.octoapp.widgets.webcam.NoControlsWebcamAppWidget
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber


class ExecuteWidgetActionActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_TASK = "de.crysxd.octoapp.widgets.progress.TASK"
        private const val EXTRA_APP_WIDGET_ID = "de.crysxd.octoapp.widgets.progress.APP_WIDGET_ID"
        private const val EXTRA_PLAY_LIVE = "de.crysxd.octoapp.widgets.progress.PLAY_LIVE"
        private const val TASK_CANCEL = "cancel"
        private const val TASK_PAUSE = "pause"
        private const val TASK_RESUME = "resume"
        private const val TASK_REFRESH = "refresh"

        fun createRefreshTaskPendingIntent(context: Context, appWidgetId: Int, playLive: Boolean) =
            createTaskPendingIntent(context, TASK_REFRESH, "ExecuteWidgetActionActivity/$TASK_REFRESH/$appWidgetId/$playLive") {
                it.putExtra(EXTRA_APP_WIDGET_ID, appWidgetId)
                it.putExtra(EXTRA_PLAY_LIVE, playLive)
            }

        fun createCancelTaskPendingIntent(context: Context) = createTaskPendingIntent(context, TASK_CANCEL)
        fun createPauseTaskPendingIntent(context: Context) = createTaskPendingIntent(context, TASK_PAUSE)
        fun createResumeTaskPendingIntent(context: Context) = createTaskPendingIntent(context, TASK_RESUME)
        private fun createTaskPendingIntent(
            context: Context,
            task: String,
            id: String = "ExecuteWidgetActionActivity/$task",
            intentUpdate: (Intent) -> Unit = {}
        ): PendingIntent =
            PendingIntent.getActivity(
                context,
                id.hashCode(),
                Intent(context, ExecuteWidgetActionActivity::class.java).also {
                    it.putExtra(EXTRA_TASK, task)
                    intentUpdate(it)
                },
                PendingIntent.FLAG_UPDATE_CURRENT
            )
    }

    private val task get() = intent.getStringExtra(EXTRA_TASK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.i("Starting ExecuteWidgetActionActivity for task $task")
        overridePendingTransition(0, 0)

        if (task == TASK_REFRESH) {
            triggerRefresh()
            finish()
        } else {
            confirmAction()
        }
    }

    private fun triggerRefresh() {
        intent.getIntExtra(EXTRA_APP_WIDGET_ID, 0).takeIf { it != 0 }?.let { id ->
            val live = intent.getBooleanExtra(EXTRA_PLAY_LIVE, false)
            Timber.i("Updating request received for $id (live=$live)")

            val manager = AppWidgetManager.getInstance(this)
            val progressWidgetIds = ComponentName(this, ProgressAppWidget::class.java).let { manager.getAppWidgetIds(it) }
            val webcamWidgetIds = listOf(
                ComponentName(this, ControlsWebcamAppWidget::class.java).let { manager.getAppWidgetIds(it) }.toList(),
                ComponentName(this, NoControlsWebcamAppWidget::class.java).let { manager.getAppWidgetIds(it) }.toList(),
            ).flatten()

            when {
                progressWidgetIds.contains(id) -> ProgressAppWidget.notifyWidgetDataChanged()
                webcamWidgetIds.contains(id) -> BaseWebcamAppWidget.updateAppWidget(this, id, live)
            }
        } ?: updateAllWidgets()

        finish()
    }

    private fun confirmAction() {
        val message = when (task) {
            TASK_CANCEL -> R.string.cancel_print_confirmation_message
            TASK_PAUSE -> R.string.pause_print_confirmation_message
            TASK_RESUME -> R.string.resume_print_confirmation_message
            else -> null
        }

        val action = when (task) {
            TASK_CANCEL -> R.string.cancel_print_confirmation_action
            TASK_PAUSE -> R.string.pause_print_confirmation_action
            TASK_RESUME -> R.string.resume_print_confirmation_action
            else -> null
        }

        if (message == null || action == null) {
            Timber.e(IllegalArgumentException("Activity started with task $task, did not find action or message"))
            finish()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setMessage(message)
            .setPositiveButton(action) { _, _ ->
                Timber.i("Task $task confirmed")
                // Activity will be finished in a millisecond, so we use Global to trigger the action
                GlobalScope.launch {
                    when (task) {
                        TASK_CANCEL -> Injector.get().cancelPrintJobUseCase().execute(CancelPrintJobUseCase.Params(false))
                        TASK_PAUSE, TASK_RESUME -> Injector.get().togglePausePrintJobUseCase().execute(Unit)
                        else -> Unit
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                finish()
            }
            .show()
    }
}