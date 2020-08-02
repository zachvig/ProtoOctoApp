package de.crysxd.octoapp.base.datasource

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV1
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import timber.log.Timber

class LocalOctoPrintInstanceInformationSource(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) : DataSource<OctoPrintInstanceInformationV2> {

    companion object {
        private const val KEY_INSTANCE_INFORMATION = "octorpint_instance_information_v2"
        private const val KEY_LEGACY_V1_INSTANCE_INFORMATION = "octorpint_instance_information"
        private const val KEY_LEGACY_V0_HOST_NAME = "octorpint_host_name"
        private const val KEY_LEGACY_V0_PORT = "octoprint_port"
        private const val KEY_LEGACY_V0_API_KEY = "octoprint_api_key"
        private const val KEY_LEGACY_V0_SUPPORTS_PSU_PLUGIN = "octoprint_supports_psu_plugin"
        private const val KEY_LEGACY_V0_API_KEY_WAS_INVALID = "octoprint_api_key_was_invalid"
    }

    override fun store(t: OctoPrintInstanceInformationV2?) = sharedPreferences.edit {
        if (t == null) {
            remove(KEY_INSTANCE_INFORMATION)
        } else {
            putString(KEY_INSTANCE_INFORMATION, gson.toJson(t))
        }
    }

    override fun get(): OctoPrintInstanceInformationV2? {
        // Upgrade V0 -> V2
        getLegacyV0()?.let {
            val v2 = OctoPrintInstanceInformationV2(it)
            Timber.i("Upgrading from V0 -> V2 (v0=$it, v2=$v2)")
            store(v2)
        }

        // Upgrade V1 -> V2
        getLegacyV1()?.let {
            val v2 = OctoPrintInstanceInformationV2(it)
            Timber.i("Upgrading from V1 -> V2 (v0=$it, v2=$v2)")
            store(v2)
        }

        // Clear all legacy
        clearLegacy()

        return sharedPreferences.getString(KEY_INSTANCE_INFORMATION, null)?.let {
            gson.fromJson(it, OctoPrintInstanceInformationV2::class.java)
        }
    }

    private fun getLegacyV1() = sharedPreferences.getString(KEY_LEGACY_V1_INSTANCE_INFORMATION, null)?.let {
        gson.fromJson(it, OctoPrintInstanceInformationV1::class.java)
    }

    private fun getLegacyV0() = if (
        sharedPreferences.contains(KEY_LEGACY_V0_API_KEY) &&
        sharedPreferences.contains(KEY_LEGACY_V0_HOST_NAME) &&
        sharedPreferences.contains(KEY_LEGACY_V0_PORT)
    ) {
        OctoPrintInstanceInformationV1(
            sharedPreferences.getString(KEY_LEGACY_V0_HOST_NAME, "") ?: "",
            sharedPreferences.getInt(KEY_LEGACY_V0_PORT, 80),
            sharedPreferences.getString(KEY_LEGACY_V0_API_KEY, "") ?: "",
            sharedPreferences.getBoolean(KEY_LEGACY_V0_SUPPORTS_PSU_PLUGIN, false),
            sharedPreferences.getBoolean(KEY_LEGACY_V0_API_KEY_WAS_INVALID, false)
        )
    } else {
        null
    }

    private fun clearLegacy() {
        sharedPreferences.edit {
            remove(KEY_LEGACY_V0_HOST_NAME)
            remove(KEY_LEGACY_V0_PORT)
            remove(KEY_LEGACY_V0_API_KEY)
            remove(KEY_LEGACY_V0_SUPPORTS_PSU_PLUGIN)
            remove(KEY_LEGACY_V0_API_KEY_WAS_INVALID)
            remove(KEY_LEGACY_V1_INSTANCE_INFORMATION)
        }
    }
}