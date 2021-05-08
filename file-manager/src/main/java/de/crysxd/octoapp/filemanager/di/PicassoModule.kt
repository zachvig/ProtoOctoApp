package de.crysxd.octoapp.filemanager.di

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import com.squareup.picasso.LruCache
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ext.resolve
import timber.log.Timber

@Module
class PicassoModule {

    @Provides
    @PrePrintControlsScope
    fun providePicasso(context: Context, octoPrintProvider: OctoPrintProvider): LiveData<Picasso?> =
        octoPrintProvider.octoPrintFlow().asLiveData().map {
            it?.let { octoPrint ->
                Picasso.Builder(context)
                    .downloader(OkHttp3Downloader(octoPrint.createOkHttpClient()))
                    .memoryCache(LruCache(context))
                    .requestTransformer { request ->
                        request.uri?.let { uri ->
                            val newUri = Uri.parse(octoPrint.webUrl)
                                .buildUpon()
                                .resolve(uri.path)
                                .query(uri.query)
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