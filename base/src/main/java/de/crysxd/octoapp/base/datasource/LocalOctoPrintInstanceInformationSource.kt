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
        private const val KEY_SUPPORTS_PSU_PLUGIN = "octoprint_supports_psu_plugin"
        private const val KEY_API_KEY_WAS_INVALID = "octoprint_api_key_was_invalid"
    }

    override fun store(t: OctoPrintInstanceInformation?) = sharedPreferences.edit {
        if (t == null) {
            remove(KEY_API_KEY)
            remove(KEY_HOST_NAME)
            remove(KEY_PORT)
            remove(KEY_SUPPORTS_PSU_PLUGIN)
            remove(KEY_API_KEY_WAS_INVALID)
        } else {
            putString(KEY_API_KEY, t.apiKey)
            putString(KEY_HOST_NAME, t.hostName)
            putInt(KEY_PORT, t.port)
            putBoolean(KEY_SUPPORTS_PSU_PLUGIN, t.supportsPsuPlugin)
            putBoolean(KEY_API_KEY_WAS_INVALID, t.apiKeyWasInvalid)
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
            sharedPreferences.getString(KEY_API_KEY, "") ?: "",
            sharedPreferences.getBoolean(KEY_SUPPORTS_PSU_PLUGIN, false),
            sharedPreferences.getBoolean(KEY_API_KEY_WAS_INVALID, false)
        )
    } else {
        null
    }
}