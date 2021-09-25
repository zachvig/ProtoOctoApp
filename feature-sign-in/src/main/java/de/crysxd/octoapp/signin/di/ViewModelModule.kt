package de.crysxd.octoapp.signin.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.crysxd.octoapp.base.data.repository.NotificationIdRepository
import de.crysxd.octoapp.base.data.repository.OctoPrintRepository
import de.crysxd.octoapp.base.di.ViewModelFactory
import de.crysxd.octoapp.base.di.ViewModelKey
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import de.crysxd.octoapp.base.usecase.OpenOctoprintWebUseCase
import de.crysxd.octoapp.base.usecase.RequestApiAccessUseCase
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import de.crysxd.octoapp.signin.access.RequestAccessViewModel
import de.crysxd.octoapp.signin.apikey.ManualApiKeyViewModel
import de.crysxd.octoapp.signin.discover.DiscoverViewModel
import de.crysxd.octoapp.signin.probe.ProbeOctoPrintViewModel
import javax.inject.Provider

@Module
open class ViewModelModule {

    @Provides
    fun bindViewModelFactory(creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>): ViewModelProvider.Factory =
        ViewModelFactory(creators)

    @Provides
    @IntoMap
    @ViewModelKey(DiscoverViewModel::class)
    open fun provideDiscoverViewModel(
        discoverOctoPrintUseCase: DiscoverOctoPrintUseCase,
        octoPrintRepository: OctoPrintRepository,
        sensitiveDataMask: SensitiveDataMask,
    ): ViewModel = DiscoverViewModel(
        discoverOctoPrintUseCase = discoverOctoPrintUseCase,
        octoPrintRepository = octoPrintRepository,
        sensitiveDataMask = sensitiveDataMask,
    )

    @Provides
    @IntoMap
    @ViewModelKey(ProbeOctoPrintViewModel::class)
    open fun provideProbeOctoPrintViewModel(
        useCase: TestFullNetworkStackUseCase,
        octoPrintRepository: OctoPrintRepository,
    ): ViewModel = ProbeOctoPrintViewModel(
        useCase = useCase,
        octoPrintRepository = octoPrintRepository
    )

    @Provides
    @IntoMap
    @ViewModelKey(ManualApiKeyViewModel::class)
    open fun provideManualApiKeyViewModel(
        useCase: TestFullNetworkStackUseCase,
    ): ViewModel = ManualApiKeyViewModel(
        testFullNetworkStackUseCase = useCase,
    )

    @Provides
    @IntoMap
    @ViewModelKey(RequestAccessViewModel::class)
    open fun provideRequestAccessViewModel(
        useCase: RequestApiAccessUseCase,
        openOctoprintWebUseCase: OpenOctoprintWebUseCase,
        notificationIdRepository: NotificationIdRepository,
    ): ViewModel = RequestAccessViewModel(
        requestApiAccessUseCase = useCase,
        openOctoprintWebUseCase = openOctoprintWebUseCase,
        notificationIdRepository = notificationIdRepository
    )
}