package de.crysxd.octoapp.base.di.modules

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.datasource.*
import de.crysxd.octoapp.base.di.BaseScope
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.octoprint.json.PluginSettingsDeserializer
import de.crysxd.octoapp.octoprint.models.settings.Settings

@Module
class DataSourceModule {

    private fun createGson() = GsonBuilder()
        .registerTypeAdapter(Settings.PluginSettingsGroup::class.java, PluginSettingsDeserializer())
        .create()

    @Provides
    fun provideLegacyOctoPrintInstanceInformationDataSource(sharedPreferences: SharedPreferences): DataSource<OctoPrintInstanceInformationV2> =
        LocalLegacyOctoPrintInstanceInformationSource(
            sharedPreferences,
            createGson()
        )

    @Provides
    fun provideOctoPrintInstanceInformationDataSource(sharedPreferences: SharedPreferences): DataSource<List<OctoPrintInstanceInformationV2>> =
        LocalOctoPrintInstanceInformationSource(
            sharedPreferences,
            createGson()
        )

    @Provides
    fun provideGcodeHistoryDataSource(sharedPreferences: SharedPreferences): DataSource<List<GcodeHistoryItem>> =
        LocalGcodeHistoryDataSource(sharedPreferences, Gson())

    @Provides
    fun provideLocalPinnedMenuItemsDataSource(sharedPreferences: SharedPreferences): LocalPinnedMenuItemsDataSource =
        LocalPinnedMenuItemsDataSource(sharedPreferences)

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