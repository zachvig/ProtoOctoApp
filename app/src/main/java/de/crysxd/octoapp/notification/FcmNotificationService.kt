package de.crysxd.octoapp.notification

import android.app.NotificationManager
import android.content.Context
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.utils.AppScope
import de.crysxd.octoapp.notification.PrintState.Companion.DEFAULT_FILE_NAME
import de.crysxd.octoapp.notification.PrintState.Companion.DEFAULT_FILE_TIME
import de.crysxd.octoapp.notification.PrintState.Companion.DEFAULT_PROGRESS
import de.crysxd.octoapp.octoprint.models.settings.Settings
import de.crysxd.octoapp.widgets.createLaunchAppIntent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import timber.log.Timber
import java.security.MessageDigest
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class FcmNotificationService : FirebaseMessagingService() {

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

        Timber.i("Received message")
        message.data["raw"]?.let {
            handleRawDataEvent(
                instanceId = message.data["instanceId"] ?: throw IllegalArgumentException("Not instance id"),
                raw = it,
                sentTime = Date(message.sentTime),
            )
        }

        message.notification?.let {
            handleNotification(it)
        }
    }

    private fun handleNotification(notification: RemoteMessage.Notification) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = getString(R.string.updates_notification_channel)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentText(notification.body)
            .setContentTitle(notification.title)
            .setSmallIcon(R.drawable.ic_notification_default)
            .setAutoCancel(true)
            .setColorized(true)
            .setColor(ContextCompat.getColor(this, R.color.primary_dark))
            .setContentIntent(createLaunchAppIntent(this, null))
        manager.notify(Injector.get().notificationIdRepository().nextUpdateNotificationId(), notificationBuilder.build())
    }

    private fun handleRawDataEvent(instanceId: String, raw: String, sentTime: Date) = AppScope.launch(exceptionHandler) {
        Timber.i("Received message with raw data for instance: $instanceId")

        // Decrypt and decode data
        val key = Injector.get().octorPrintRepository().get(instanceId)?.settings?.plugins?.values?.mapNotNull {
            it as? Settings.OctoAppCompanionSettings
        }?.firstOrNull()?.encryptionKey ?: throw IllegalStateException("No encryption key present")
        val decrypted = AESCipher(key).decrypt(raw)
        val data = Gson().fromJson(String(decrypted), FcmPrintEvent::class.java)

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
        fileDate = fileDate ?: DEFAULT_FILE_TIME,
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