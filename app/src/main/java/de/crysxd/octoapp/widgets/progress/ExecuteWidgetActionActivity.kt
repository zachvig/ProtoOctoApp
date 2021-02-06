package de.crysxd.octoapp.widgets.progress

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.usecase.CancelPrintJobUseCase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber


class ExecuteWidgetActionActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_TASK = "de.crysxd.octoapp.widgets.progress.TASK"
        private const val TASK_CANCEL = "cancel"
        private const val TASK_PAUSE = "pause"
        private const val TASK_RESUME = "resume"

        fun createCancelTaskPendingIntent(context: Context) = createTaskPendingIntent(context, TASK_CANCEL)
        fun createPauseTaskPendingIntent(context: Context) = createTaskPendingIntent(context, TASK_PAUSE)
        fun createResumeTaskPendingIntent(context: Context) = createTaskPendingIntent(context, TASK_RESUME)
        private fun createTaskPendingIntent(context: Context, task: String): PendingIntent = PendingIntent.getActivity(
            context,
            "ExecuteWidgetActionActivity/$task".hashCode(),
            Intent(context, ExecuteWidgetActionActivity::class.java).also { it.putExtra(EXTRA_TASK, task) },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private val task get() = intent.getStringExtra(EXTRA_TASK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.i("Starting ExecuteWidgetActionActivity for task $task")
        overridePendingTransition(0, 0)

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