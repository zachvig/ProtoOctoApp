package de.crysxd.octoapp.base

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformation
import de.crysxd.octoapp.octoprint.OctoPrint
import okhttp3.logging.HttpLoggingInterceptor

class OctoPrintRepository(
    private val sharedPreferences: SharedPreferences,
    private val httpLoggingInterceptor: HttpLoggingInterceptor
) {

    private val mutableOctoprint = MutableLiveData<OctoPrint>()
    val octoprint = Transformations.map(mutableOctoprint) { it }

    init {
        storeOctoprintInstanceInformation(getOctoprintInstanceInformation())
    }

    companion object {
        private const val KEY_HOST_NAME = "octorpint_host_name"
        private const val KEY_PORT = "octoprint_port"
        private const val KEY_API_KEY = "octoprint_api_key"
    }

    fun storeOctoprintInstanceInformation(instance: OctoPrintInstanceInformation) {
        sharedPreferences.edit {
            putString(KEY_API_KEY, instance.apiKey)
            putString(KEY_HOST_NAME, instance.hostName)
            putInt(KEY_PORT, instance.port)
        }

        mutableOctoprint.postValue(getOctoprint(instance))
    }

    private fun getOctoprintInstanceInformation() = OctoPrintInstanceInformation(
        sharedPreferences.getString(KEY_HOST_NAME, "") ?: "",
        sharedPreferences.getInt(KEY_PORT, -1),
        sharedPreferences.getString(KEY_API_KEY, "") ?: ""
    )

    fun isInstanceInformationAvailable() : Boolean {
        val info = getOctoprintInstanceInformation()
        return !(info.apiKey.isBlank() || info.hostName.isBlank() || info.port <= 0)
    }

    fun getOctoprint(instance: OctoPrintInstanceInformation) = OctoPrint(
        instance.hostName,
        instance.port,
        instance.apiKey,
        listOf(httpLoggingInterceptor)
    )
}