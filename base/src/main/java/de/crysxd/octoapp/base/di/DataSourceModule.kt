package de.crysxd.octoapp.base.di

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.datasource.LocalOctoPrintInstanceInformationSource
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformation

@Module
class DataSourceModule {

    @Provides
    fun provideOctoPrintInstanceInformationDataSource(sharedPreferences: SharedPreferences): DataSource<OctoPrintInstanceInformation> =
        LocalOctoPrintInstanceInformationSource(sharedPreferences)

}