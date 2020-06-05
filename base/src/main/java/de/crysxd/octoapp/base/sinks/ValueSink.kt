package de.crysxd.octoapp.base.sinks

import android.os.Parcelable

interface ValueSink : Parcelable {

    fun useValue(value: String)

}