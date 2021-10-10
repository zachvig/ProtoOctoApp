package de.crysxd.octoapp.base.data.source

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV1
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV2
import de.crysxd.octoapp.base.data.models.OctoPrintInstanceInformationV3
import de.crysxd.octoapp.base.logging.SensitiveDataMask
import timber.log.Timber


class LocalOctoPrintInstanceInformationSource(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson,
    private val sensitiveDataMask: SensitiveDataMask,
) : DataSource<List<OctoPrintInstanceInformationV3>> {

    companion object {
        // V1 Model in original store
        private const val KEY_LEGACY_V0_HOST_NAME = "octorpint_host_name"
        private const val KEY_LEGACY_V0_PORT = "octoprint_port"
        private const val KEY_LEGACY_V0_API_KEY = "octoprint_api_key"
        private const val KEY_LEGACY_V0_SUPPORTS_PSU_PLUGIN = "octoprint_supports_psu_plugin"
        private const val KEY_LEGACY_V0_API_KEY_WAS_INVALID = "octoprint_api_key_was_invalid"

        // V1 model in single store
        private const val KEY_LEGACY_INSTANCE_INFORMATION_V1 = "octorpint_instance_information"

        // V2 model in single store
        private const val KEY_LEGACY_INSTANCE_INFORMATION_V2S = "octorpint_instance_information_v2"

        // V2 model in list store
        private const val KEY_LEGACY_INSTANCE_INFORMATION_V2L = "octorpint_instance_information_v3"

        // V3 model
        private const val KEY_INSTANCE_INFORMATION_V3 = "octorpint_instance_information_v4"

        // Notification id range
        private val NOTIFICATION_ID_RANGE = 15_000..15_999
    }

    override fun store(t: List<OctoPrintInstanceInformationV3>?) = if (t == null) {
        sharedPreferences.edit { remove(KEY_INSTANCE_INFORMATION_V3) }
    } else {
        sharedPreferences.edit { putString(KEY_INSTANCE_INFORMATION_V3, gson.toJson(t)) }
    }

    override fun get(): List<OctoPrintInstanceInformationV3> = try {
        try {
            upgradeV0ToV3()
            upgradeV1ToV3()
            upgradeV2ToV3()
            upgradeWithNotificationId()
        } catch (e: Exception) {
            Timber.e(e)
        } finally {
            clearAllLegacy()
        }

        // Load
        gson.fromJson(
            sharedPreferences.getString(KEY_INSTANCE_INFORMATION_V3, "[]"),
            object : TypeToken<List<OctoPrintInstanceInformationV3>>() {}.type
        )
    } catch (e: Exception) {
        Timber.e(e)
        emptyList()
    }

    private fun upgradeV0ToV3() = if (
        sharedPreferences.contains(KEY_LEGACY_V0_API_KEY) &&
        sharedPreferences.contains(KEY_LEGACY_V0_HOST_NAME) &&
        sharedPreferences.contains(KEY_LEGACY_V0_PORT)
    ) {
        // Load
        val v1 = OctoPrintInstanceInformationV1(
            sharedPreferences.getString(KEY_LEGACY_V0_HOST_NAME, "") ?: "",
            sharedPreferences.getInt(KEY_LEGACY_V0_PORT, 80),
            sharedPreferences.getString(KEY_LEGACY_V0_API_KEY, "") ?: "",
            sharedPreferences.getBoolean(KEY_LEGACY_V0_SUPPORTS_PSU_PLUGIN, false),
            sharedPreferences.getBoolean(KEY_LEGACY_V0_API_KEY_WAS_INVALID, false)
        )

        // Upgrade
        val v3 = OctoPrintInstanceInformationV3(OctoPrintInstanceInformationV2(v1))
        sensitiveDataMask.registerInstance(v3)
        Timber.i("Upgrading from V0 -> V3 (v0=$v1, v3=$v3)")
        store(listOf(v3))
    } else {
        null
    }

    private fun upgradeV1ToV3() {
        if (sharedPreferences.contains(KEY_LEGACY_INSTANCE_INFORMATION_V1)) {
            sharedPreferences.getString(KEY_LEGACY_INSTANCE_INFORMATION_V1, null)?.let {
                gson.fromJson(it, OctoPrintInstanceInformationV1::class.java)
            }?.let {
                val v3 = OctoPrintInstanceInformationV3(OctoPrintInstanceInformationV2(it))
                sensitiveDataMask.registerInstance(v3)
                Timber.i("Upgrading from V1 -> V3 (v1=$it, v3=$v3)")
                store(listOf(v3))
            }
        }
    }

    private fun upgradeV2ToV3() {
        // Signle item update
        if (sharedPreferences.contains(KEY_LEGACY_INSTANCE_INFORMATION_V2S)) {
            sharedPreferences.getString(KEY_LEGACY_INSTANCE_INFORMATION_V2S, null)?.let {
                gson.fromJson(it, OctoPrintInstanceInformationV2::class.java)
            }?.let {
                val v3 = OctoPrintInstanceInformationV3(it)
                sensitiveDataMask.registerInstance(v3)
                Timber.i("Upgrading from single V2 -> V3 (v2=$it, v3=$v3)")
                store(listOf(v3))
            }
        }

        // List update
        if (sharedPreferences.contains(KEY_LEGACY_INSTANCE_INFORMATION_V2L)) {
            val v2 = gson.fromJson<List<OctoPrintInstanceInformationV2>>(
                sharedPreferences.getString(KEY_LEGACY_INSTANCE_INFORMATION_V2L, "[]"),
                object : TypeToken<List<OctoPrintInstanceInformationV2>>() {}.type
            )

            v2.map {
                OctoPrintInstanceInformationV3(it)
            }.let {
                // Store and delete
                it.forEachIndexed { index, v3 ->
                    sensitiveDataMask.registerInstance(v3)
                    Timber.i("Upgrading from V2 -> V3 (v2=${v2[index]}, v3=$v3)")
                }
                store(it)
            }
        }
    }

    private fun upgradeWithNotificationId() {
        // Load
        val instances = gson.fromJson<List<OctoPrintInstanceInformationV3>>(
            sharedPreferences.getString(KEY_INSTANCE_INFORMATION_V3, "[]"),
            object : TypeToken<List<OctoPrintInstanceInformationV3>>() {}.type
        ).map { it.id to it }.toMap().toMutableMap()

        // Generate unique notification ids
        instances.values.forEach {
            if (it.notificationId == null) {
                // New id: max + 1
                val id = NOTIFICATION_ID_RANGE.first + ((instances.maxOf {
                    it.value.notificationId ?: NOTIFICATION_ID_RANGE.first
                } + 1) % NOTIFICATION_ID_RANGE.last)
                instances[it.id] = it.copy(notificationId = id)
            }
        }

        // Store
        store(instances.values.toList())
    }

    private fun clearAllLegacy() {
        sharedPreferences.edit {
            remove(KEY_LEGACY_V0_HOST_NAME)
            remove(KEY_LEGACY_V0_PORT)
            remove(KEY_LEGACY_V0_API_KEY)
            remove(KEY_LEGACY_V0_SUPPORTS_PSU_PLUGIN)
            remove(KEY_LEGACY_V0_API_KEY_WAS_INVALID)
            remove(KEY_LEGACY_INSTANCE_INFORMATION_V1)
            remove(KEY_LEGACY_INSTANCE_INFORMATION_V2L)
            remove(KEY_LEGACY_INSTANCE_INFORMATION_V2S)
        }
    }
}