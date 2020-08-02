package de.crysxd.octoapp.base.di.modules

import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.datasource.LocalOctoPrintInstanceInformationSource
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2

@Module
class DataSourceModule {

    @Provides
    fun provideOctoPrintInstanceInformationDataSource(sharedPreferences: SharedPreferences): DataSource<OctoPrintInstanceInformationV2> =
        LocalOctoPrintInstanceInformationSource(sharedPreferences, Gson())

}