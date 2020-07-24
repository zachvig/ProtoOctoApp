package de.crysxd.octoapp.base.di.modules

import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.InvalidApiKeyInterceptor
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.datasource.DataSource
import de.crysxd.octoapp.base.di.BaseScope
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformation
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.base.usecase.CheckOctoPrintInstanceInformationUseCase
import okhttp3.logging.HttpLoggingInterceptor

@Module
open class OctoPrintModule {

    @BaseScope
    @Provides
    open fun provideOctoPrintRepository(
        dataSource: DataSource<OctoPrintInstanceInformation>,
        checkOctoPrintInstanceInformationUseCase: CheckOctoPrintInstanceInformationUseCase
    ) = OctoPrintRepository(
        dataSource,
        checkOctoPrintInstanceInformationUseCase
    )

    @BaseScope
    @Provides
    open fun provideOctoPrintProvider(
        httpLoggingInterceptor: HttpLoggingInterceptor,
        invalidApiKeyInterceptor: InvalidApiKeyInterceptor,
        octoPrintRepository: OctoPrintRepository,
        analytics: FirebaseAnalytics
    ) = OctoPrintProvider(httpLoggingInterceptor, invalidApiKeyInterceptor, octoPrintRepository, analytics)

    @BaseScope
    @Provides
    open fun provideInvalidApiKeyInterceptor(
        octoPrintRepository: OctoPrintRepository
    ) = InvalidApiKeyInterceptor(octoPrintRepository)

}