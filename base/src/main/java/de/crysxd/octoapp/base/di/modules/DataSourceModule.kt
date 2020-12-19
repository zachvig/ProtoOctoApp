package de.crysxd.octoapp.base.di.modules

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.datasource.*
import de.crysxd.octoapp.base.gcode.parse.GcodeParser
import de.crysxd.octoapp.base.models.GcodeHistoryItem
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2

@Module
class DataSourceModule {

    @Provides
    fun provideOctoPrintInstanceInformationDataSource(sharedPreferences: SharedPreferences): DataSource<OctoPrintInstanceInformationV2> =
        LocalOctoPrintInstanceInformationSource(sharedPreferences, Gson())

    @Provides
    fun provideGcodeHistoryDataSource(sharedPreferences: SharedPreferences): DataSource<List<GcodeHistoryItem>> =
        LocalGcodeHistoryDataSource(sharedPreferences, Gson())

    @Provides
    fun providesGcodeFileDataSources(
        context: Context,
        octoPrintProvider: OctoPrintProvider
    ): GcodeFileDataSourceGroup {
        val memory = MemoryGcodeFileDataSource()
        val local = LocalGcodeFileDataSource(context, Gson(), context.getSharedPreferences("gcode_cache_index", Context.MODE_PRIVATE))
        val remote = RemoteGcodeFileDataSource(GcodeParser(), octoPrintProvider)
        return GcodeFileDataSourceGroup(listOf(memory, local, remote))
    }
}