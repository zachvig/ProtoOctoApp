package de.crysxd.octoapp.framework.rules

import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.usecase.RequestApiAccessUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking

class AcceptAllAccessRequestRule(val instanceInformation: OctoPrintInstanceInformationV2) : AbstractUseCaseMockRule() {

    override fun createBaseComponent(base: BaseComponent) = MockBaseComponent(base)

    inner class MockBaseComponent(real: BaseComponent) : BaseComponent by real {
        override fun requestApiAccessUseCase() = mock<RequestApiAccessUseCase>().also {
            runBlocking {
                whenever(it.execute(any())).thenAnswer {
                    // Check correct web url
                    val params = it.arguments.mapNotNull { it as? RequestApiAccessUseCase.Params }.first()
                    Truth.assertThat(params.webUrl).isEqualTo(instanceInformation.webUrl)

                    // Return flow
                    flow {
                        repeat(5) {
                            emit(RequestApiAccessUseCase.State.Pending)
                            delay(400)
                        }
                        emit(RequestApiAccessUseCase.State.AccessGranted(apiKey = instanceInformation.apiKey))
                    }
                }
            }
        }
    }
}