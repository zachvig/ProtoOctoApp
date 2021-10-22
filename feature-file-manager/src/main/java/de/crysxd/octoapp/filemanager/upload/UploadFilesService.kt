package de.crysxd.octoapp.filemanager.upload

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.filemanager.R
import de.crysxd.octoapp.filemanager.di.FileManagerInjector
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class UploadFilesService : Service() {

    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(job + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, t -> Timber.e(t) })
    private val mediator = FileManagerInjector.get().uploadMediator()
    private val notificationId = BaseInjector.get().notificationIdRepository().getUploadStatusNotificationId()
    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        Timber.i("Upload service started")
        startForeground(notificationId, createNotification())

        coroutineScope.launch {
            while (true) {
                val next = mediator.getActiveUploads().firstOrNull() ?: return@launch stopSelf()
                Timber.i("Picking next upload: ${next.id}")
                performUpload(next)
                Timber.i("Upload completed")
                mediator.endUpload(next.id)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        Timber.i("Upload service stopped")
        stopForeground(true)
    }

    private suspend fun performUpload(upload: Upload) {
        try {
            delay(15000)
            notificationManager.notify(notificationId, createNotification(progress = 0f, name = upload.name))
            var lastProgressLog = 0
            var lastProgressUpdate = 0
            upload.fileApi.uploadFile(
                origin = upload.origin,
                parent = upload.parent,
                name = upload.name,
                source = upload.source,
                progressUpdate = {
                    val percent = (it * 100).toInt()
                    if (lastProgressLog + 10 <= percent) {
                        lastProgressLog = percent
                        Timber.i("Progress for ${upload.id}: $percent")
                    }

                    if (percent > lastProgressUpdate) {
                        lastProgressUpdate = percent
                        notificationManager.notify(notificationId, createNotification(progress = it, name = upload.name))
                        upload.updateProgress(it)
                    }
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Upload ${upload.id} failed")
        }
    }

    private fun createNotification(progress: Float = 0f, name: String? = null) = NotificationCompat.Builder(this, getString(R.string.updates_notification_channel))
        .setContentTitle(name?.let { "Uploading $it **" } ?: "Starting upload...")
        .setSmallIcon(R.drawable.ic_notification_default)
        .setSilent(true)
        .setProgress(100, (progress * 100).toInt(), progress == 0f || progress == 1f)
        .setContentText(
            mediator.getActiveUploads().size.let {
                if (it > 1) "$it files pending" else ""
            }
        ).build()
}