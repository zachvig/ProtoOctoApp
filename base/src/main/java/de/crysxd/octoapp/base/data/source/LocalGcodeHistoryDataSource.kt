package de.crysxd.octoapp.base.data.source

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.crysxd.octoapp.base.data.models.GcodeHistoryItem
import timber.log.Timber

class LocalGcodeHistoryDataSource(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) : DataSource<List<GcodeHistoryItem>> {

    companion object {
        private const val KEY = "gcode_history"
    }

    override fun store(t: List<GcodeHistoryItem>?) {
        try {
            sharedPreferences.edit {
                putString(KEY, gson.toJson(t))
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun get(): List<GcodeHistoryItem>? = if (sharedPreferences.contains(KEY)) {
        gson.fromJson(
            sharedPreferences.getString(KEY, "[]"),
            object : TypeToken<List<GcodeHistoryItem>>() {}.type
        )
    } else {
        null
    }
}