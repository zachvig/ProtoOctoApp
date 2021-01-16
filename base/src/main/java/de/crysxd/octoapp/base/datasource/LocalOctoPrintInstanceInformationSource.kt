package de.crysxd.octoapp.base.datasource

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.crysxd.octoapp.base.models.OctoPrintInstanceInformationV2
import timber.log.Timber


private const val KEY_INSTANCE_INFORMATION = "octorpint_instance_information_v3"

class LocalOctoPrintInstanceInformationSource(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) : DataSource<List<OctoPrintInstanceInformationV2>> {

    override fun store(t: List<OctoPrintInstanceInformationV2>?) = if (t == null) {
        sharedPreferences.edit { remove(KEY_INSTANCE_INFORMATION) }
    } else {
        sharedPreferences.edit { putString(KEY_INSTANCE_INFORMATION, gson.toJson(t)) }
    }

    override fun get(): List<OctoPrintInstanceInformationV2> = try {
        gson.fromJson(
            sharedPreferences.getString(KEY_INSTANCE_INFORMATION, "[]"),
            object : TypeToken<List<OctoPrintInstanceInformationV2>>() {}.type
        )
    } catch (e: Exception) {
        Timber.e(e)
        emptyList()
    }
}