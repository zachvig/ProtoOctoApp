package de.crysxd.octoapp.base.di.modules

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.di.modules.FileModule.PublicFileFactory
import java.io.File

@Module
class FileModule {

    @Provides
    fun providePublicFileFactory(context: Context, cacheDir: File) = PublicFileFactory { fileName: String ->
        val f = File(cacheDir, fileName)
        f.createNewFile()
        f.deleteOnExit()
        val uri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", f)
        f to uri
    }

    @Provides
    fun providePublicCacheDir(context: Context) = File(context.externalCacheDir, context.getString(R.string.public_file_dir_name)).also {
        it.mkdirs()
    }

    fun interface PublicFileFactory {
        fun createPublicFile(fileName: String): Pair<File, Uri>
    }
}