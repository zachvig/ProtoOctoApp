package de.crysxd.octoapp.base.ui.widget.progress

import androidx.lifecycle.ViewModel
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.livedata.OctoTransformations.filter
import de.crysxd.octoapp.base.livedata.OctoTransformations.filterEventsForMessageType
import de.crysxd.octoapp.octoprint.models.socket.Message

class ProgressWidgetViewModel(
    private val octoPrintProvider: OctoPrintProvider
) : ViewModel() {

    val printState = octoPrintProvider.eventLiveData
        .filterEventsForMessageType(Message.CurrentMessage::class.java)
        .filter { it.progress != null }

}