package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.core.app.ShareCompat
import de.crysxd.octoapp.base.di.modules.FileModule
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.models.files.FileObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class DownloadAndShareFileUseCase @Inject constructor(
    private val octoPrintProvider: OctoPrintProvider,
    private val publicFileFactory: FileModule.PublicFileFactory,
) : UseCase<DownloadAndShareFileUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) = withContext(Dispatchers.IO) {
        // Create cache dir and make sure we delete all old files
        val (file, uri) = publicFileFactory.createPublicFile(param.file.name)

        try {
            // Download
            timber.i("Downloading ${param.file.path}")
            octoPrintProvider.octoPrint().createFilesApi().downloadFile(param.file)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Share
            val mimeType = param.file.extension?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
            timber.i("Sharing ${param.file.path} from $uri with mime type $mimeType")
            ShareCompat.IntentBuilder(param.context)
                .setStream(uri)
                .setChooserTitle(param.file.name)
                .setType(mimeType ?: "file/*")
                .startChooser()
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    data class Params(
        val context: Context,
        val file: FileObject.File,
    )
}