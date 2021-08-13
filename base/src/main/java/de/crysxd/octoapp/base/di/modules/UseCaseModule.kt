package de.crysxd.octoapp.base.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase

@Module
class UseCaseModule {

    @Provides
    fun provideDiscoverOctoPrintUseCase(
        context: Context,
        sensitiveData: SensitiveDataMask,
        testFullNetworkStackUseCase: TestFullNetworkStackUseCase,
    ) = DiscoverOctoPrintUseCase(
        context = context,
        sensitiveDataMask = sensitiveData,
        testFullNetworkStackUseCase = testFullNetworkStackUseCase
    )
}