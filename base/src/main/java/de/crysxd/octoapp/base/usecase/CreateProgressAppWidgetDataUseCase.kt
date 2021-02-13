package de.crysxd.octoapp.base.usecase

import android.os.Parcelable
import de.crysxd.octoapp.base.OctoPrintProvider
import de.crysxd.octoapp.base.repository.OctoPrintRepository
import de.crysxd.octoapp.octoprint.models.socket.Message
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class CreateProgressAppWidgetDataUseCase @Inject constructor(
    private val octoPrintRepository: OctoPrintRepository,
    private val octoPrintProvider: OctoPrintProvider
) : UseCase<CreateProgressAppWidgetDataUseCase.Params, CreateProgressAppWidgetDataUseCase.Result>() {

    init {
        //suppressLogging = true
    }

    override suspend fun doExecute(param: Params, timber: Timber.Tree) =
        param.currentMessage?.let { fromCurrentMessage(it, param.webUrl) }
            ?: fromNetworkRequest(param.webUrl)

    private suspend fun fromNetworkRequest(webUrl: String): Result = withContext(Dispatchers.IO) {
        val instance = octoPrintRepository.getAll().firstOrNull { it.webUrl == webUrl } ?: throw IllegalStateException("Unable to locate instance for $webUrl")
        val octoPrint = octoPrintProvider.createAdHocOctoPrint(instance)
        val asyncState = async { octoPrint.createPrinterApi().getPrinterState() }
        val asyncJob = async { octoPrint.createJobApi().getJob() }

        val state = asyncState.await()
        val job = asyncJob.await()

        return@withContext Result(
            isPrinting = state.state?.flags?.printing == true,
            isPausing = state.state?.flags?.pausing == true,
            isCancelling = state.state?.flags?.cancelling == true,
            isPaused = state.state?.flags?.paused == true,
            isLive = false,
            printProgress = job.progress?.completion?.let { it / 100f },
            printTimeLeft = job.progress?.printTimeLeft,
            printTimeLeftOrigin = job.progress?.printTimeLeftOrigin,
            webUrl = webUrl,
        )
    }

    private fun fromCurrentMessage(currentMessage: Message.CurrentMessage, webUrl: String) = Result(
        isPrinting = currentMessage.state?.flags?.printing == true,
        isPausing = currentMessage.state?.flags?.pausing == true,
        isCancelling = currentMessage.state?.flags?.cancelling == true,
        isPaused = currentMessage.state?.flags?.paused == true,
        isLive = true,
        printProgress = currentMessage.progress?.completion?.let { it / 100f },
        printTimeLeft = currentMessage.progress?.printTimeLeft,
        printTimeLeftOrigin = currentMessage.progress?.printTimeLeftOrigin,
        webUrl = webUrl,
    )

    data class Params(
        val currentMessage: Message.CurrentMessage?,
        val webUrl: String
    )

    @Parcelize
    data class Result(
        val isPrinting: Boolean,
        val isPausing: Boolean,
        val isPaused: Boolean,
        val isCancelling: Boolean,
        val isLive: Boolean,
        val printProgress: Float?,
        val printTimeLeft: Int?,
        val printTimeLeftOrigin: String?,
        val webUrl: String,
        val createdAt: Date = Date(),
    ) : Parcelable
}