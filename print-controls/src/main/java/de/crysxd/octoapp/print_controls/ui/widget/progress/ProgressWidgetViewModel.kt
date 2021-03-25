package de.crysxd.octoapp.base.ui.widget.progress

import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ext.rateLimit
import de.crysxd.octoapp.base.ui.base.BaseViewModel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class ProgressWidgetViewModel(
    octoPrintProvider: OctoPrintProvider
) : BaseViewModel() {

    val printState = octoPrintProvider.passiveCurrentMessageFlow("progress_widget")
        .filter { it.progress != null }
        .rateLimit(5000)
        .asLiveData()

}