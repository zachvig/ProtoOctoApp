package de.crysxd.octoapp.base.di.modules

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.data.models.GcodeHistoryItem
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.data.source.DataSource
import de.crysxd.octoapp.base.data.source.LocalGcodeFileDataSource
import de.crysxd.octoapp.base.data.source.LocalGcodeHistoryDataSource
import de.crysxd.octoapp.base.data.source.LocalOctoPrintInstanceInformationSource
import de.crysxd.octoapp.base.data.source.LocalPinnedMenuItemsDataSource
import de.crysxd.octoapp.base.data.source.RemoteGcodeFileDataSource
import de.crysxd.octoapp.base.data.source.RemoteTutorialsDataSource
import de.crysxd.octoapp.base.data.source.WidgetPreferencesDataSource
import de.crysxd.octoapp.base.di.BaseScope
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.network.OctoPrintProvider
import de.crysxd.octoapp.octoprint.json.HttpUrlAdapter
import de.crysxd.octoapp.octoprint.json.PluginSettingsDeserializer
import de.crysxd.octoapp.octoprint.models.settings.Settings
import okhttp3.HttpUrl

@Module
class DataSourceModule {

    private fun createGson() = GsonBuilder()
        .registerTypeAdapter(Settings.PluginSettingsGroup::class.java, PluginSettingsDeserializer())
        .registerTypeAdapter(HttpUrl::class.java, HttpUrlAdapter())
        .create()

    @Provides
    fun provideOctoPrintInstanceInformationDataSource(
        sharedPreferences: SharedPreferences,
        sensitiveDataMask: SensitiveDataMask,
    ): DataSource<List<OctoPrintInstanceInformationV3>> = LocalOctoPrintInstanceInformationSource(
        sharedPreferences,
        createGson(),
        sensitiveDataMask
    )

    @Provides
    fun provideGcodeHistoryDataSource(sharedPreferences: SharedPreferences): DataSource<List<GcodeHistoryItem>> =
        LocalGcodeHistoryDataSource(sharedPreferences, Gson())

    @Provides
    fun provideLocalPinnedMenuItemsDataSource(
        sharedPreferences: SharedPreferences,
        octoPrintRepository: OctoPrintRepository,
    ): LocalPinnedMenuItemsDataSource = LocalPinnedMenuItemsDataSource(
        sharedPreferences = sharedPreferences,
        octoPrintRepository = octoPrintRepository
    )

    @Provides
    @BaseScope
    fun provideLocalGcodeFileDataSource(
        context: Context,
    ) = LocalGcodeFileDataSource(context, Gson(), context.getSharedPreferences("gcode_cache_index_3", Context.MODE_PRIVATE))

    @Provides
    @BaseScope
    fun provideRemoteGcodeFileDataSource(
        octoPrintProvider: OctoPrintProvider,
        local: LocalGcodeFileDataSource
    ) = RemoteGcodeFileDataSource(octoPrintProvider, local)

    @Provides
    @BaseScope
    fun provideRemoteTutorialsDataSource() = RemoteTutorialsDataSource()

    @Provides
    @BaseScope
    fun provideWidgetOrderDataSource(
        sharedPreferences: SharedPreferences,
    ) = WidgetPreferencesDataSource(
        sharedPreferences = sharedPreferences,
        gson = createGson()
    )
}