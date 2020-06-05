package de.crysxd.octoapp.base.di.modules

import dagger.Module
import dagger.Provides
import de.crysxd.octoapp.base.sinks.SendGcodeCommandValueSink

@Module
class ValueSinkModules {

    @Provides
    fun provideSendGcodeValueSink() =
        SendGcodeCommandValueSink()

}