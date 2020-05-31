package de.crysxd.octoapp.base.datasource

import android.content.SharedPreferences
import androidx.core.content.edit
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformation

class LocalOctoPrintInstanceInformationSource(private val sharedPreferences: SharedPreferences) :
    DataSource<OctoPrintInstanceInformation> {

    companion object {
        private const val KEY_HOST_NAME = "octorpint_host_name"
        private const val KEY_PORT = "octoprint_port"
        private const val KEY_API_KEY = "octoprint_api_key"
    }

    override fun store(t: OctoPrintInstanceInformation?) = sharedPreferences.edit {
        if (t == null) {
            remove(KEY_API_KEY)
            remove(KEY_HOST_NAME)
            remove(KEY_PORT)
        } else {
            putString(KEY_API_KEY, t.apiKey)
            putString(KEY_HOST_NAME, t.hostName)
            putInt(KEY_PORT, t.port)
        }
    }

    override fun get() = if (
        sharedPreferences.contains(KEY_API_KEY) &&
        sharedPreferences.contains(KEY_HOST_NAME) &&
        sharedPreferences.contains(KEY_PORT)
    ) {
        OctoPrintInstanceInformation(
            sharedPreferences.getString(KEY_HOST_NAME, "") ?: "",
            sharedPreferences.getInt(KEY_PORT, -1),
            sharedPreferences.getString(KEY_API_KEY, "") ?: ""
        )
    } else {
        null
    }
}