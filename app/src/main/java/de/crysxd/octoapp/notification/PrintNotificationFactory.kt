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
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.ui.utils.colorTheme
import de.crysxd.octoapp.base.usecase.FormatEtaUseCase
import de.crysxd.octoapp.widgets.createLaunchAppIntent
import java.util.Locale

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
            NotificationChannelGroup(OCTOPRINT_CHANNEL_GROUP_ID, "Print progress")
        )

        // Create missing notification channels
        octoPrintRepository.getAll().forEach {
            if (!notificationManager.notificationChannels.any { c -> c.id == it.channelId }) {
                createNotificationChannel(name = it.label, id = it.channelId, groupId = OCTOPRINT_CHANNEL_GROUP_ID)
            }
        }

        // Create filament change channel
        createNotificationChannel(
            id = FILAMENT_CHANGE_CHANNEL_ID,
            name = getString(R.string.notification_channel_filament_change),
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        name: String,
        id: String,
        groupId: String? = null,
        soundUri: Uri? = Uri.parse("android.resource://${packageName}/${R.raw.notification_filament_change}"),
        audioAttributes: AudioAttributes? = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build(),
        importance: Int = NotificationManager.IMPORTANCE_HIGH,
    ) = notificationManager.createNotificationChannel(
        NotificationChannel(id, name, importance).also { nc ->
            nc.group = groupId
            soundUri?.let { uri ->
                audioAttributes?.let { attrs ->
                    nc.setSound(uri, attrs)
                }
            }
        }
    )

    fun createServiceNotification(
        instanceInformation: OctoPrintInstanceInformationV3,
        statusText: String
    ) = createNotificationBuilder(
        instanceInformation = instanceInformation,
        notificationChannelId = instanceInformation.channelId
    ).setContentTitle(statusText)
        .setSilent(true)
        .build()

    suspend fun createStatusNotification(
        instanceId: String,
        print: Print,
    ) = octoPrintRepository.get(instanceId)?.let {
        createNotificationBuilder(
            instanceInformation = it,
            notificationChannelId = it.channelId
        ).setContentTitle(print.notificationTitle)
            .setContentText(print.notificationText(it))
            .setProgress(MAX_PROGRESS, (MAX_PROGRESS * (print.progress / 100f)).toInt(), false)
            .addStopLiveAction(print)
            .setSilent(true)
            .build()
    }

    fun createFilamentChangeNotification(
        instanceId: String,
    ) = octoPrintRepository.get(instanceId)?.let {
        createNotificationBuilder(
            instanceInformation = it,
            notificationChannelId = FILAMENT_CHANGE_CHANNEL_ID
        ).setContentTitle("${it.label} needs filament")
            .setContentText("Print was paused")
            .setAutoCancel(true)
            .build()
    }

    fun createPrintCompletedNotification(
        instanceId: String,
        print: Print
    ) = octoPrintRepository.get(instanceId)?.let {
        createNotificationBuilder(instanceInformation = it, notificationChannelId = it.channelId)
            .setContentTitle("${it.label} completed print")
            .setContentText(print.fileName)
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

    private val Print.notificationTitle
        get() = when (state) {
            Print.State.Printing -> String.format(Locale.getDefault(), "Printing: %.0f%%", progress)
            Print.State.Pausing -> "Pausing"
            Print.State.Paused -> "Paused"
            Print.State.Cancelling -> "Cancelling"
        }

    private suspend fun Print.notificationText(instanceInformation: OctoPrintInstanceInformationV3) = listOfNotNull(
        "Live".takeIf { source == Print.Source.Live && !BillingManager.isFeatureEnabled(BillingManager.FEATURE_QUICK_SWITCH) },
        "Live on ${instanceInformation.label}".takeIf { source == Print.Source.Live && BillingManager.isFeatureEnabled(BillingManager.FEATURE_QUICK_SWITCH) },
        instanceInformation.label.takeIf { source != Print.Source.Live && BillingManager.isFeatureEnabled(BillingManager.FEATURE_QUICK_SWITCH) },
        eta?.let {
            formatEtaUseCase.execute(
                FormatEtaUseCase.Params(
                    secsLeft = it.time - System.currentTimeMillis(),
                    showLabel = true,
                    allowRelative = false
                )
            )
        }
    ).joinToString()

    private val OctoPrintInstanceInformationV3.channelId get() = "$OCTOPRINT_CHANNEL_PREFIX${id}"

    private fun NotificationCompat.Builder.addStopLiveAction(print: Print) = if (print.source != Print.Source.Live) {
        // Cancel if this is not a live notification
        this
    } else {
        addAction(
            NotificationCompat.Action.Builder(
                null,
                "Pause live updates",
                PendingIntent.getService(
                    this@PrintNotificationFactory,
                    0,
                    Intent(
                        this@PrintNotificationFactory,
                        PrintNotificationService::class.java
                    ).setAction(ACTION_STOP),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            ).build()
        )
    }
}
