package de.crysxd.baseui.di

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
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.resolvePath
import timber.log.Timber

@Module
class PicassoModule {

    @Provides
    @BaseUiScope
    fun providePicasso(context: Context, octoPrintProvider: OctoPrintProvider): LiveData<Picasso?> =
        octoPrintProvider.octoPrintFlow().asLiveData().map {
            it?.let { octoPrint ->
                Picasso.Builder(context)
                    .downloader(OkHttp3Downloader(octoPrint.createOkHttpClient()))
                    .memoryCache(LruCache(context))
                    .requestTransformer { request ->
                        if (request.uri.scheme == "file") {
                            request
                        } else {
                            request.uri?.let { uri ->
                                val newUri = octoPrint.webUrl
                                    .resolvePath(uri.path)
                                    .newBuilder()
                                    .query(uri.query)
                                    .build()

                                Timber.d("Mapping $uri -> $newUri")

                                request.buildUpon()
                                    .setUri(Uri.parse(newUri.toString()))
                                    .build()
                            } ?: request
                        }
                    }.build()
            }
        }
}