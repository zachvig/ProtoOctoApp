package de.crysxd.octoapp.base.ext

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import timber.log.Timber


fun Uri.open(context: Context) {
    Timber.i("Opening link: $this")
    val intent = Intent(Intent.ACTION_VIEW, this).also { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Unable to open link, no app found", Toast.LENGTH_SHORT).show()
    }
}