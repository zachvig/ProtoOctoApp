package de.crysxd.octoapp.base.sinks

import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ui.common.enter_value.EnterValueFragment
import de.crysxd.octoapp.octoprint.models.printer.GcodeCommand
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Parcelize
class SendGcodeCommandValueSink : ValueSink {

    override fun useValue(value: String) {
        GlobalScope.launch {
            Injector.get().octoPrintProvider().octoPrint.value?.let {
                val gcodeCommand = GcodeCommand.Batch(value.split("\n").toTypedArray())
                Injector.get().executeGcodeCommandUseCase().execute(Pair(it, gcodeCommand))
            }
        }
    }
}