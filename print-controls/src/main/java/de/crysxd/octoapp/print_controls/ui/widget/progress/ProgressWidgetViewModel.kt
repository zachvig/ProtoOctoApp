package de.crysxd.octoapp.base.ui.widget.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.ext.rateLimit
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class ProgressWidgetViewModel(
    octoPrintProvider: OctoPrintProvider
) : ViewModel() {

    val printState = octoPrintProvider.passiveCurrentMessageFlow("progress_widget")
        .filter { it.progress != null }
        .rateLimit(5000)
        .onEach { Timber.i("Pass") }
        .asLiveData()

}