package de.crysxd.octoapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.mainActivityClass
import de.crysxd.octoapp.base.models.exceptions.UserMessageException
import de.crysxd.octoapp.base.ui.base.LocalizedActivity
import de.crysxd.octoapp.base.ui.menu.ConfirmedMenuItem
import de.crysxd.octoapp.base.ui.menu.Menu
import de.crysxd.octoapp.base.ui.menu.MenuHost
import de.crysxd.octoapp.base.ui.menu.MenuItem
import de.crysxd.octoapp.base.ui.menu.main.MenuItemLibrary
import de.crysxd.octoapp.base.ui.widget.WidgetHostFragment
import de.crysxd.octoapp.base.usecase.CancelPrintJobUseCase
import de.crysxd.octoapp.base.utils.AppScope
import de.crysxd.octoapp.base.utils.PendingIntentCompat
import de.crysxd.octoapp.widgets.progress.ProgressAppWidget
import de.crysxd.octoapp.widgets.webcam.BaseWebcamAppWidget
import de.crysxd.octoapp.widgets.webcam.ControlsWebcamAppWidget
import de.crysxd.octoapp.widgets.webcam.NoControlsWebcamAppWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


class ExecuteWidgetActionActivity : LocalizedActivity(), MenuHost {

    companion object {
        private const val EXTRA_TASK = "de.crysxd.octoapp.widgets.progress.TASK"
        private const val EXTRA_APP_WIDGET_ID = "de.crysxd.octoapp.widgets.progress.APP_WIDGET_ID"
        private const val EXTRA_PLAY_LIVE = "de.crysxd.octoapp.widgets.progress.PLAY_LIVE"
        private const val EXTRA_MENU_ITEM_ID = "de.crysxd.octoapp.widgets.progress.MENU_ITEM_ID"
        private const val TASK_CANCEL = "cancel"
        private const val TASK_PAUSE = "pause"
        private const val TASK_RESUME = "resume"
        private const val TASK_REFRESH = "refresh"
        private const val TASK_CLICK_MENU_ITEM = "click"

        fun createRefreshTaskPendingIntent(context: Context, appWidgetId: Int, playLive: Boolean) =
            createTaskPendingIntent(context, TASK_REFRESH, "ExecuteWidgetActionActivity/$TASK_REFRESH/$appWidgetId/$playLive") {
                it.putExtra(EXTRA_APP_WIDGET_ID, appWidgetId)
                it.putExtra(EXTRA_PLAY_LIVE, playLive)
            }

        fun createCancelTaskPendingIntent(context: Context) = createTaskPendingIntent(context, TASK_CANCEL)
        fun createPauseTaskPendingIntent(context: Context) = createTaskPendingIntent(context, TASK_PAUSE)
        fun createResumeTaskPendingIntent(context: Context) = createTaskPendingIntent(context, TASK_RESUME)
        fun createClickMenuItemPendingIntentTemplate(context: Context) = createTaskPendingIntent(context, TASK_CLICK_MENU_ITEM)
        fun createClickMenuItemFillIntent(menuItemId: String) = Intent().also {
            it.putExtra(EXTRA_MENU_ITEM_ID, menuItemId)
        }

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
                PendingIntentCompat.FLAG_UPDATE_CURRENT_IMMUTABLE
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
                webcamWidgetIds.contains(id) -> BaseWebcamAppWidget.updateAppWidget(id, live, true)
            }
        } ?: updateAllWidgets()

        finish()
    }

    private fun confirmAction() = lifecycleScope.launchWhenCreated {
        val menuItem = MenuItemLibrary()[intent.getStringExtra(EXTRA_MENU_ITEM_ID) ?: ""]

        val message = when (task) {
            TASK_CANCEL -> getString(R.string.cancel_print_confirmation_message)
            TASK_PAUSE -> getString(R.string.pause_print_confirmation_message)
            TASK_RESUME -> getString(R.string.resume_print_confirmation_message)
            TASK_CLICK_MENU_ITEM -> getString(
                R.string.app_widget___click_menu_item_confirmation_message,
                menuItem?.getTitle(this@ExecuteWidgetActionActivity)
            )
            else -> null
        }

        val action = when (task) {
            TASK_CANCEL -> R.string.cancel_print_confirmation_action
            TASK_PAUSE -> R.string.pause_print_confirmation_action
            TASK_RESUME -> R.string.resume_print_confirmation_action
            TASK_CLICK_MENU_ITEM -> R.string.app_widget___click_menu_item_confirmation_action
            else -> null
        }

        if (message == null || action == null) {
            Timber.e(IllegalArgumentException("Activity started with task $task, did not find action or message"))
            finish()
            return@launchWhenCreated
        }

        MaterialAlertDialogBuilder(this@ExecuteWidgetActionActivity)
            .setMessage(message)
            .setPositiveButton(action) { _, _ ->
                Timber.i("Task $task confirmed")
                // Activity will be finished in a millisecond, so we use Global to trigger the action
                AppScope.launch(Dispatchers.Main) {
                    try {
                        when (task) {
                            TASK_CANCEL -> Injector.get().cancelPrintJobUseCase().execute(CancelPrintJobUseCase.Params(false))
                            TASK_PAUSE, TASK_RESUME -> Injector.get().togglePausePrintJobUseCase().execute(Unit)
                            TASK_CLICK_MENU_ITEM -> menuItem?.let { performMenuItemClick(it) }
                            else -> Unit
                        }
                    } catch (e: Exception) {
                        Timber.e(e)
                        Toast.makeText(Injector.get().context(), "Failed to execute task", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                finish()
            }
            .show()
    }

    private suspend fun performMenuItemClick(menuItem: MenuItem) {
        // This is very ugly...
        mainActivityClass = MainActivity::class.java

        try {
            Timber.i("Executing ${menuItem.itemId}")
            if (menuItem is ConfirmedMenuItem) {
                menuItem.onConfirmed(this)
            } else {
                menuItem.onClicked(this)
            }
            Toast.makeText(
                this,
                getString(R.string.menu___completed_command, menuItem.getTitle(this)),
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Timber.e(e)
            Toast.makeText(
                this,
                (e as? UserMessageException)?.userMessage?.let(::getString) ?: e.localizedMessage ?: "Something went wrong",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun requireContext() = this

    override fun pushMenu(subMenu: Menu) = Unit

    override fun closeMenu() = Unit

    override fun getNavController(): NavController? = null

    override fun getMenuActivity() = this

    override fun getMenuFragmentManager(): FragmentManager? = null

    override fun getWidgetHostFragment(): WidgetHostFragment? = null

    override fun reloadMenu() = Unit

    override fun isCheckBoxChecked() = false
}