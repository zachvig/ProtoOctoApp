package de.crysxd.octoapp.notification

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import de.crysxd.baseui.utils.colorTheme
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import de.crysxd.octoapp.base.utils.PendingIntentCompat
import de.crysxd.octoapp.widgets.createLaunchAppIntent

class PrintNotificationFactory(
    context: Context,
    private val octoPrintRepository: OctoPrintRepository,
    private val formatEtaUseCase: FormatEtaUseCase,
) : ContextWrapper(context) {

    companion object {
        private const val OCTOPRINT_CHANNEL_PREFIX = "octoprint_"
        private const val OCTOPRINT_CHANNEL_GROUP_ID = "octoprint"
        private const val FILAMENT_CHANGE_CHANNEL_ID = "filament_change"
        private const val MAX_PROGRESS = 1000
        private const val OPEN_APP_REQUEST_CODE = 3249
    }

    private val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannels() {
        // Delete legacy channel and channels for deleted instances
        notificationManager.deleteNotificationChannel("print_progress")
        notificationManager.notificationChannels.filter { it.id.startsWith(OCTOPRINT_CHANNEL_PREFIX) }.forEach {
            val instance = octoPrintRepository.get(it.id.removePrefix(OCTOPRINT_CHANNEL_PREFIX))
            if (instance == null) {
                notificationManager.deleteNotificationChannel(it.id)
            }
        }

        // Create OctoPrint group
        notificationManager.createNotificationChannelGroup(
            NotificationChannelGroup(OCTOPRINT_CHANNEL_GROUP_ID, getString(R.string.notification_channel___print_progress))
        )

        // Create missing notification channels
        octoPrintRepository.getAll().forEach {
            if (!notificationManager.notificationChannels.any { c -> c.id == it.channelId }) {
                createNotificationChannel(
                    name = it.label,
                    vibrationPattern = arrayOf(0L), // Needed for Wear OS. Otherwise every percent change vibrates.
                    id = it.channelId,
                    groupId = OCTOPRINT_CHANNEL_GROUP_ID,
                    soundUri = Uri.parse("android.resource://${packageName}/${R.raw.notification_print_done}"),
                )
            }
        }

        // Create filament change channel
        createNotificationChannel(
            id = FILAMENT_CHANGE_CHANNEL_ID,
            soundUri = Uri.parse("android.resource://${packageName}/${R.raw.notification_filament_change}"),
            name = getString(R.string.notification_channel___filament_change),
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        name: String,
        id: String,
        groupId: String? = null,
        soundUri: Uri? = null,
        vibrationPattern: Array<Long>? = null,
        audioAttributes: AudioAttributes? = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build(),
        importance: Int = NotificationManager.IMPORTANCE_HIGH,
    ) = notificationManager.createNotificationChannel(
        NotificationChannel(id, name, importance).also { nc ->
            nc.group = groupId
            vibrationPattern?.let {
                nc.vibrationPattern = it.toLongArray()
            }
            soundUri?.let { uri ->
                audioAttributes?.let { attrs ->
                    nc.setSound(uri, attrs)
                }
            }
        }
    )

    fun createServiceNotification(
        instanceInformation: OctoPrintInstanceInformationV3?,
        statusText: String
    ) = createNotificationBuilder(
        instanceInformation = instanceInformation,
        notificationChannelId = instanceInformation?.channelId ?: getString(R.string.updates_notification_channel)
    ).setContentTitle(statusText)
        .setSilent(true)
        .build()

    suspend fun createStatusNotification(
        instanceId: String,
        printState: PrintState,
        stateText: String?,
    ) = octoPrintRepository.get(instanceId)?.let {
        createNotificationBuilder(
            instanceInformation = it,
            notificationChannelId = it.channelId
        ).setContentTitle(printState.notificationTitle(stateText))
            .setContentText(printState.notificationText(it))
            .setProgress(MAX_PROGRESS, (MAX_PROGRESS * (printState.progress / 100f)).toInt(), false)
            .addStopLiveAction()
            .setSilent(true)
            .setVibrate(arrayOf(0L).toLongArray())
            .build()
    }

    fun createFilamentChangeNotification(
        instanceId: String,
    ) = octoPrintRepository.get(instanceId)?.let {
        createNotificationBuilder(
            instanceInformation = it,
            notificationChannelId = FILAMENT_CHANGE_CHANNEL_ID
        ).setContentTitle(getString(R.string.print_notification___filament_change_required_title, it.label))
            .setContentText(getString(R.string.print_notification___filament_change_required_message))
            .setAutoCancel(true)
            .build()
    }

    fun createPrintCompletedNotification(
        instanceId: String,
        printState: PrintState
    ) = octoPrintRepository.get(instanceId)?.let {
        createNotificationBuilder(instanceInformation = it, notificationChannelId = it.channelId)
            .setContentTitle(getString(R.string.print_notification___print_done_title, it.label))
            .setContentText(printState.fileName)
            .setAutoCancel(true)
            .build()
    }

    private fun createNotificationBuilder(
        instanceInformation: OctoPrintInstanceInformationV3?,
        notificationChannelId: String
    ) = NotificationCompat.Builder(this, notificationChannelId)
        .setSmallIcon(R.drawable.ic_notification_default)
        .setContentIntent(createLaunchAppIntent(this, instanceInformation?.id))
        .also {
            instanceInformation?.colorTheme?.dark?.let { color ->
                it.setColorized(true)
                it.color = color
            }
        }

    private fun PrintState.notificationTitle(stateText: String?): String {
        val title = when (state) {
            PrintState.State.Printing -> getString(R.string.print_notification___printing_title, progress)
            PrintState.State.Pausing -> getString(R.string.print_notification___pausing_title)
            PrintState.State.Paused -> getString(R.string.print_notification___paused_title)
            PrintState.State.Cancelling -> getString(R.string.print_notification___cancelling_title)
            PrintState.State.Idle -> ""
        }

        return stateText?.let {
            "$title ($stateText)"
        } ?: title
    }

    private fun isMultiPrinterActive() = octoPrintRepository.getAll().size > 1 && BillingManager.isFeatureEnabled(BillingManager.FEATURE_QUICK_SWITCH)

    private suspend fun PrintState.notificationText(instanceInformation: OctoPrintInstanceInformationV3) = listOfNotNull(
        getString(R.string.print_notification___live)
            .takeIf { source == PrintState.Source.Live && !isMultiPrinterActive() },
        getString(R.string.print_notification___live_on_x, instanceInformation.label)
            .takeIf { source == PrintState.Source.Live && isMultiPrinterActive() },
        instanceInformation.label
            .takeIf { source != PrintState.Source.Live && isMultiPrinterActive() },
        eta?.let {
            formatEtaUseCase.execute(
                FormatEtaUseCase.Params(
                    secsLeft = (it.time - System.currentTimeMillis()) / 1000,
                    showLabel = true,
                    allowRelative = false
                )
            )
        }
    ).joinToString()

    private val OctoPrintInstanceInformationV3.channelId get() = "$OCTOPRINT_CHANNEL_PREFIX${id}"

    private fun NotificationCompat.Builder.addStopLiveAction() = addAction(
        NotificationCompat.Action.Builder(
            null,
            getString(R.string.print_notification___close),
            PendingIntent.getBroadcast(
                this@PrintNotificationFactory,
                0,
                Intent(
                    this@PrintNotificationFactory,
                    PrintNotificationSupportBroadcastReceiver::class.java
                ).setAction(
                    PrintNotificationSupportBroadcastReceiver.ACTION_DISABLE_PRINT_NOTIFICATION_UNTIL_NEXT_LAUNCH
                ),
                PendingIntentCompat.FLAG_UPDATE_CURRENT_IMMUTABLE
            )
        ).build()
    )
}
