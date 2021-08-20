package de.crysxd.octoapp.framework.rules

import android.net.Uri
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.whenever
import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.base.usecase.TestFullNetworkStackUseCase
import kotlinx.coroutines.runBlocking

class MockTestFullNetworkStackRule : AbstractUseCaseMockRule() {
    private val mock: TestFullNetworkStackUseCase = mock()

    override fun createBaseComponent(base: BaseComponent): MockBaseComponent {
        reset(mock)
        return MockBaseComponent(base)
    }

    fun mockForInvalidApiKey() = runBlocking {
        whenever(mock.execute(any())).thenAnswer {
            val param = it.arguments[1] as TestFullNetworkStackUseCase.Params
            TestFullNetworkStackUseCase.Finding.InvalidApiKey(
                webUrl = param.webUrl,
                host = Uri.parse(param.webUrl).host!!
            )
        }
    }

    fun mockForHostNotReachable() = runBlocking {
        whenever(mock.execute(any())).thenAnswer {
            val param = it.arguments[1] as TestFullNetworkStackUseCase.Params
            TestFullNetworkStackUseCase.Finding.HostNotReachable(
                webUrl = param.webUrl,
                host = Uri.parse(param.webUrl).host!!,
                ip = "0.0.0.0",
                timeoutMs = 4200
            )
        }
    }

    inner class MockBaseComponent(real: BaseComponent) : BaseComponent by real {
        override fun testFullNetworkStackUseCase() = mock
    }
}