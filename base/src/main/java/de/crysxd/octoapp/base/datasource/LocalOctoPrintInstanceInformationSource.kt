package de.crysxd.octoapp.base.datasource

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformation

class LocalOctoPrintInstanceInformationSource(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) : DataSource<OctoPrintInstanceInformation> {

    companion object {
        private const val KEY_INSTANCE_INFORMATION = "octorpint_instance_information"
        private const val KEY_LEGACY_HOST_NAME = "octorpint_host_name"
        private const val KEY_LEGACY_PORT = "octoprint_port"
        private const val KEY_LEGACY_API_KEY = "octoprint_api_key"
        private const val KEY_LEGACY_SUPPORTS_PSU_PLUGIN = "octoprint_supports_psu_plugin"
        private const val KEY_LEGACY_API_KEY_WAS_INVALID = "octoprint_api_key_was_invalid"
    }

    override fun store(t: OctoPrintInstanceInformation?) = sharedPreferences.edit {
        if (t == null) {
            remove(KEY_INSTANCE_INFORMATION)
        } else {
            putString(KEY_INSTANCE_INFORMATION, gson.toJson(t))
        }
    }

    override fun get(): OctoPrintInstanceInformation? {
        // Upgrade
        getLegacy()?.let {
            store(it)
            clearLegacy()
        }

        return sharedPreferences.getString(KEY_INSTANCE_INFORMATION, null)?.let {
            gson.fromJson(it, OctoPrintInstanceInformation::class.java)
        }
    }

    private fun getLegacy() = if (
        sharedPreferences.contains(KEY_LEGACY_API_KEY) &&
        sharedPreferences.contains(KEY_LEGACY_HOST_NAME) &&
        sharedPreferences.contains(KEY_LEGACY_PORT)
    ) {
        OctoPrintInstanceInformation(
            sharedPreferences.getString(KEY_LEGACY_HOST_NAME, "") ?: "",
            sharedPreferences.getInt(KEY_LEGACY_PORT, 80),
            sharedPreferences.getString(KEY_LEGACY_API_KEY, "") ?: "",
            sharedPreferences.getBoolean(KEY_LEGACY_SUPPORTS_PSU_PLUGIN, false),
            sharedPreferences.getBoolean(KEY_LEGACY_API_KEY_WAS_INVALID, false)
        )
    } else {
        null
    }

    private fun clearLegacy() {
        sharedPreferences.edit {
            remove(KEY_LEGACY_HOST_NAME)
            remove(KEY_LEGACY_PORT)
            remove(KEY_LEGACY_API_KEY)
            remove(KEY_LEGACY_SUPPORTS_PSU_PLUGIN)
            remove(KEY_LEGACY_API_KEY_WAS_INVALID)
        }
    }
}