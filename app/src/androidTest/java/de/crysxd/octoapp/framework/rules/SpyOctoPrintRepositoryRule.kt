package de.crysxd.octoapp.framework.rules

import com.nhaarman.mockitokotlin2.spy
import de.crysxd.octoapp.base.di.BaseComponent

class SpyOctoPrintRepositoryRule : AbstractUseCaseMockRule() {

    override fun createBaseComponent(base: BaseComponent) = MockBaseComponent(base)

    inner class MockBaseComponent(real: BaseComponent) : BaseComponent by real {
        private val spy = spy(real.octorPrintRepository())
        override fun octorPrintRepository() = spy
    }
}