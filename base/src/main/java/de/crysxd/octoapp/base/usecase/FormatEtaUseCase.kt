package de.crysxd.octoapp.base.usecase

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FormatEtaUseCase @Inject constructor() : UseCase<Int, String> {

    override suspend fun execute(param: Int): String {
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