package de.crysxd.octoapp.base.datasource

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.crysxd.octoapp.base.models.GcodeHistoryItem

class LocalGcodeHistoryDataSource(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) : DataSource<List<GcodeHistoryItem>> {

    companion object {
        private const val KEY = "gcode_history"
    }

    override fun store(t: List<GcodeHistoryItem>?) {
        sharedPreferences.edit {
            putString(KEY, gson.toJson(t))
        }
    }

    override fun get(): List<GcodeHistoryItem> = gson.fromJson(
        sharedPreferences.getString(KEY, "[]"),
        object : TypeToken<ArrayList<List<GcodeHistoryItem>>>() {}.type
    ) ?: emptyList()
}