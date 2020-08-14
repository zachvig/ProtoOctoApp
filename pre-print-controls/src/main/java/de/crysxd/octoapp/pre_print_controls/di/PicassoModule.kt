package de.crysxd.octoapp.pre_print_controls.di

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.squareup.picasso.LruCache
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ext.appendFullPath
import timber.log.Timber

@Module
class PicassoModule {

    @Provides
    @PrePrintControlsScope
    fun providePicasso(context: Context, octoPrintProvider: OctoPrintProvider): LiveData<Picasso?> =
        octoPrintProvider.octoPrint.map {
            it?.let { octoPrint ->
                Picasso.Builder(context)
                    .downloader(OkHttp3Downloader(octoPrint.createOkHttpClient()))
                    .memoryCache(LruCache(context))
                    .requestTransformer { request ->
                        request.uri?.let { uri ->
                            val newUri = Uri.parse(octoPrint.webUrl)
                                .buildUpon()
                                .appendFullPath(uri.path)
                                .build()

                            Timber.d("Mapping $uri -> $newUri")

                            request.buildUpon()
                                .setUri(newUri)
                                .build()
                        } ?: request
                    }
                    .build()
            }
        }
}