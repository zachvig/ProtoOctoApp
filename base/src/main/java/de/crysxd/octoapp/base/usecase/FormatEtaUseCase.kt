package de.crysxd.octoapp.base.usecase

import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FormatEtaUseCase @Inject constructor() : UseCase<Int, String>() {

    init {
        suppressLogging = true
    }

    override suspend fun doExecute(param: Int, timber: Timber.Tree): String {
        val eta = Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(param.toLong()))
        return if (eta.isToday()) {
            DateFormat.getTimeInstance(DateFormat.SHORT).format(eta)
        } else {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(eta)
        }
    }

    private fun Date.isToday(): Boolean {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        return format.format(Date()) == format.format(this)
    }
}