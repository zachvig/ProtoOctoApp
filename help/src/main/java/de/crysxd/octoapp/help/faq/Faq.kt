package de.crysxd.octoapp.help.faq

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize

@Parcelize
data class Faq(
    val id: String?,
    val hidden: Boolean?,
    val title: String?,
    val content: String?,
    val youtubeUrl: String?,
    val youtubeThumbnailUrl: String?,
) : Parcelable

fun parseFaqsFromJson(json: String) = Gson().fromJson<List<Faq>>(json, object : TypeToken<ArrayList<Faq>>() {}.type)
