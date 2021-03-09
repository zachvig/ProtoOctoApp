package de.crysxd.octoapp.base.datasource

import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import de.crysxd.octoapp.base.models.WidgetOrder
import timber.log.Timber

class WidgetOrderDataSource(
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) {

    fun store(listId: String, order: WidgetOrder) {
        Timber.i("Storing order: $order")
        sharedPreferences.edit {
            putString("widget_order_$listId", gson.toJson(order))
        }
    }

    fun load(listId: String): WidgetOrder? {
        val string = sharedPreferences.getString("widget_order_$listId", null)
        return gson.fromJson(string, WidgetOrder::class.java)
    }
}
