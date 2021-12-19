package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.core.app.ShareCompat
import de.crysxd.octoapp.base.data.repository.TimelapseRepository
import de.crysxd.octoapp.base.di.modules.FileModule
import de.crysxd.octoapp.octoprint.models.timelapse.TimelapseFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class DownloadAndShareTimelapseUseCase @Inject constructor(
    private val timelapseRepository: TimelapseRepository,
    private val publicFileFactory: FileModule.PublicFileFactory,
) : UseCase<DownloadAndShareTimelapseUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) = withContext(Dispatchers.IO) {
        // Create cache dir and make sure we delete all old files
        val (shareFile, uri) = publicFileFactory.createPublicFile(param.file.name ?: "unknown")

        try {
            // Download
            timber.i("Downloading ${param.file.url}")
            val remoteFile = requireNotNull(timelapseRepository.download(param.file)) { "Unable to obtain remote file" }
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(remoteFile.name)
            remoteFile.inputStream().use { input ->
                shareFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Share
            timber.i("Sharing ${param.file.url} from $uri with mime type $mimeType")
            ShareCompat.IntentBuilder(param.context)
                .setStream(uri)
                .setChooserTitle(param.file.name)
                .setType(mimeType ?: "file/*")
                .startChooser()
        } catch (e: Exception) {
            shareFile.delete()
            throw e
        }
    }

    data class Params(
        val context: Context,
        val file: TimelapseFile,
    )
}