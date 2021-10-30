package de.crysxd.octoapp.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.MainActivity.Companion.EXTRA_CLICK_URI
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.utils.AppScope
import de.crysxd.octoapp.base.utils.PendingIntentCompat
import de.crysxd.octoapp.notification.PrintState.Companion.DEFAULT_FILE_NAME
import de.crysxd.octoapp.notification.PrintState.Companion.DEFAULT_FILE_TIME
import de.crysxd.octoapp.notification.PrintState.Companion.DEFAULT_PROGRESS
import de.crysxd.octoapp.octoprint.models.settings.Settings
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import timber.log.Timber
import java.security.MessageDigest
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class FcmNotificationService : FirebaseMessagingService() {

    companion object {
        private var lastUpdateServerTime: Long = 0L
    }

    private val notificationController by lazy { PrintNotificationController.instance }
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.i("New token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        try {
            Timber.i("Received message")
            message.data["raw"]?.let {
                handleRawDataEvent(
                    instanceId = message.data["instanceId"] ?: throw IllegalArgumentException("Not instance id"),
                    raw = it,
                    sentTime = Date(message.sentTime),
                )
            }

            message.notification?.let {
                handleNotification(it, message.data[EXTRA_CLICK_URI])
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun handleNotification(notification: RemoteMessage.Notification, contextUri: String?) {
        Timber.i("Showing notification")
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = getString(R.string.updates_notification_channel)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentText(notification.body)
            .setContentTitle(notification.title)
            .setSmallIcon(R.drawable.ic_notification_default)
            .setAutoCancel(true)
            .setColorized(true)
            .setColor(ContextCompat.getColor(this, R.color.primary_dark))
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    notification.title.hashCode(),
                    Intent(this, MainActivity::class.java).also { it.putExtra(EXTRA_CLICK_URI, contextUri) },
                    PendingIntentCompat.FLAG_IMMUTABLE
                )
            )
        manager.notify(BaseInjector.get().notificationIdRepository().nextUpdateNotificationId(), notificationBuilder.build())
    }

    private fun handleRawDataEvent(instanceId: String, raw: String, sentTime: Date) = AppScope.launch(exceptionHandler) {
        Timber.i("Received message with raw data for instance: $instanceId")

        // Check if for active instance or feature enabled
        val isForActive = BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()?.id == instanceId
        if (!isForActive && !BillingManager.isFeatureEnabled(BillingManager.FEATURE_QUICK_SWITCH)) {
            Timber.i("Dropping message for $instanceId as it's not the active instance and quick switch is disabled")
            return@launch
        }

        // Decrypt and decode data
        val key = BaseInjector.get().octorPrintRepository().get(instanceId)?.settings?.plugins?.values?.mapNotNull {
            it as? Settings.OctoAppCompanionSettings
        }?.firstOrNull()?.encryptionKey ?: throw IllegalStateException("No encryption key present")
        val decrypted = AESCipher(key).decrypt(raw)
        val data = Gson().fromJson(String(decrypted), FcmPrintEvent::class.java)
        Timber.i("Data: $data")

        val serverTime = data.serverTime ?: 0
        val previousLastServerTime = lastUpdateServerTime
        lastUpdateServerTime = serverTime

        // Handle event
        when (data.type) {
            FcmPrintEvent.Type.Completed -> notificationController.notifyCompleted(
                instanceId = instanceId,
                printState = data.toPrintState(sentTime)
            )

            FcmPrintEvent.Type.FilamentRequired -> notificationController.notifyFilamentRequired(
                instanceId = instanceId,
                printState = data.toPrintState(sentTime)
            )

            FcmPrintEvent.Type.Idle -> notificationController.notifyIdle(
                instanceId = instanceId
            )

            FcmPrintEvent.Type.Paused,
            FcmPrintEvent.Type.Printing -> {
                if (previousLastServerTime > serverTime) {
                    Timber.i("Skipping update, last server time was $lastUpdateServerTime which is after ${data.serverTime}")
                }

                notificationController.update(
                    instanceId = instanceId,
                    printState = data.toPrintState(sentTime)
                )
            }
        }
    }

    private fun FcmPrintEvent.toPrintState(sentTime: Date) = PrintState(
        source = PrintState.Source.Remote,
        progress = progress ?: DEFAULT_PROGRESS,
        appTime = Date(),
        sourceTime = sentTime,
        fileName = fileName ?: DEFAULT_FILE_NAME,
        fileDate = DEFAULT_FILE_TIME,
        eta = timeLeft?.let { Date(sentTime.time + timeLeft * 1000) },
        state = when (type) {
            FcmPrintEvent.Type.Printing -> PrintState.State.Printing
            FcmPrintEvent.Type.Paused -> PrintState.State.Paused
            FcmPrintEvent.Type.FilamentRequired -> PrintState.State.Paused
            FcmPrintEvent.Type.Completed -> PrintState.State.Idle
            FcmPrintEvent.Type.Idle -> PrintState.State.Idle
        }
    )

    private class AESCipher(private val key: String) {

        private fun createCipher(mode: Int, ivBytes: ByteArray): Cipher {
            val c = Cipher.getInstance("AES/CBC/PKCS7Padding")
            val sk = SecretKeySpec(key.getSha256(), "AES")
            val iv = IvParameterSpec(ivBytes)
            c.init(Cipher.DECRYPT_MODE, sk, iv)
            return c
        }

        fun decrypt(data: String): ByteArray {
            val bytes = Base64.decode(data, Base64.DEFAULT)
            val ivBytes = bytes.take(16).toByteArray()
            val rawDataBytes = bytes.drop(16).toByteArray()
            val cipher = createCipher(Cipher.DECRYPT_MODE, ivBytes)
            return cipher.doFinal(rawDataBytes)
        }

        private fun String.getSha256(): ByteArray {
            val digest = MessageDigest.getInstance("SHA-256").also { it.reset() }
            return digest.digest(this.toByteArray())
        }
    }
}