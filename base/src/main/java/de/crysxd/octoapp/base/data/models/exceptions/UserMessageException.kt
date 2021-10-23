package de.crysxd.octoapp.base.data.models.exceptions

import android.content.Context

interface UserMessageException {
    fun getUserMessage(context: Context): CharSequence
}