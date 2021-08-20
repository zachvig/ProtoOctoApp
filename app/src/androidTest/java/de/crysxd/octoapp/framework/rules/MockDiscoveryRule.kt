package de.crysxd.octoapp.framework.rules

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.whenever
import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.base.usecase.DiscoverOctoPrintUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.net.InetAddress

class MockDiscoveryRule : AbstractUseCaseMockRule() {

    val discoverUseCase = mock<DiscoverOctoPrintUseCase>()

    init {
        mockForNothingFound()
    }

    override fun createBaseComponent(base: BaseComponent) = MockBaseComponent(base)

    fun mockForNothingFound() = runBlocking {
        reset(discoverUseCase)
        whenever(discoverUseCase.execute(Unit)).thenReturn(flowOf(DiscoverOctoPrintUseCase.Result(emptyList())))
    }

    fun mockForRandomFound() = runBlocking {
        reset(discoverUseCase)
        whenever(discoverUseCase.execute(Unit)).thenReturn(
            flowOf(
                DiscoverOctoPrintUseCase.Result(
                    listOf(
                        baseOption,
                        baseOption.copy(label = "Terrier"),
                        baseOption.copy(label = "Beagle"),
                        baseOption.copy(label = "Dachshund"),
                    )
                )
            )
        )
    }

    private val baseOption = DiscoverOctoPrintUseCase.DiscoveredOctoPrint(
        label = "Frenchie",
        detailLabel = "Discovered via mock",
        host = InetAddress.getLocalHost(),
        quality = 100,
        method = DiscoverOctoPrintUseCase.DiscoveryMethod.DnsSd,
        port = -1,
        webUrl = "https://frenchie.com"
    )

    inner class MockBaseComponent(real: BaseComponent) : BaseComponent by real {
        override fun discoverOctoPrintUseCase(): DiscoverOctoPrintUseCase = discoverUseCase
    }
}