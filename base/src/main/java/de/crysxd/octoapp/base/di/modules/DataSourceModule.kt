package de.crysxd.octoapp.base.di.modules

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.datasource.LocalGcodeFileDataSource
import de.crysxd.octoapp.base.datasource.LocalGcodeHistoryDataSource
import de.crysxd.octoapp.base.datasource.LocalOctoPrintInstanceInformationSource
import de.crysxd.octoapp.base.datasource.LocalPinnedMenuItemsDataSource
import de.crysxd.octoapp.base.datasource.RemoteGcodeFileDataSource
import de.crysxd.octoapp.base.datasource.WidgetPreferencesDataSource
import de.crysxd.octoapp.base.di.BaseScope
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.repository.OctoPrintRepository
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
    fun provideWidgetOrderDataSource(
        sharedPreferences: SharedPreferences,
    ) = WidgetPreferencesDataSource(
        sharedPreferences = sharedPreferences,
        gson = createGson()
    )
}