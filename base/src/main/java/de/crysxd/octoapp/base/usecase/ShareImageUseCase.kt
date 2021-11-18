package de.crysxd.octoapp.base.usecase

import android.content.Context
import android.graphics.Bitmap
import androidx.core.app.ShareCompat
import de.crysxd.octoapp.base.di.modules.FileModule
import timber.log.Timber
import javax.inject.Inject

class ShareImageUseCase @Inject constructor(
    private val publicFileFactory: FileModule.PublicFileFactory,
) : UseCase<ShareImageUseCase.Params, Unit>() {

    override suspend fun doExecute(param: Params, timber: Timber.Tree) {
        // Create file and store image
        val fileName = "${param.imageName}.jpg"
        val (file, uri) = publicFileFactory.createPublicFile(fileName)
        file.outputStream().use {
            param.bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it)
        }

        // Share
        val mimeType = "image/jpeg"
        timber.i("Sharing image from $uri with mime type $mimeType")
        ShareCompat.IntentBuilder(param.context)
            .setStream(uri)
            .setChooserTitle(param.imageName)
            .setType(mimeType)
            .startChooser()
    }

    data class Params(
        val context: Context,
        val imageName: String,
        val bitmap: Bitmap,
    )
}