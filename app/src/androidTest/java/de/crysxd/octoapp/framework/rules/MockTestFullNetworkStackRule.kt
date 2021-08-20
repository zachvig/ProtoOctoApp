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
        mockLocalForDnsFailure()
        return MockBaseComponent(base)
    }

    fun mockForInvalidApiKey() {
        runBlocking {
            whenever(mock.execute(any())).thenAnswer {
                val param = it.arguments[0] as TestFullNetworkStackUseCase.Target.OctoPrint
                TestFullNetworkStackUseCase.Finding.InvalidApiKey(
                    webUrl = param.webUrl,
                    host = Uri.parse(param.webUrl).host!!
                )
            }
        }
    }

    fun mockLocalForDnsFailure() {
        runBlocking {
            whenever(mock.execute(any())).thenAnswer {
                val param = it.arguments[0] as TestFullNetworkStackUseCase.Target.OctoPrint
                TestFullNetworkStackUseCase.Finding.LocalDnsFailure(
                    webUrl = param.webUrl,
                    host = Uri.parse(param.webUrl).host!!,
                )
            }
        }
    }

    inner class MockBaseComponent(real: BaseComponent) : BaseComponent by real {
        override fun testFullNetworkStackUseCase() = mock
    }
}