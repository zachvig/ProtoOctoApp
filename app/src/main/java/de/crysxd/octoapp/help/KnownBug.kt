package de.crysxd.octoapp.help

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize

@Parcelize
class KnownBug(
    val title: String?,
    val status: String?,
    val content: String?
) : Parcelable

fun parseKnownBugsFromJson(json: String) = Gson().fromJson<List<KnownBug>>(json, object : TypeToken<ArrayList<KnownBug>>() {}.type)