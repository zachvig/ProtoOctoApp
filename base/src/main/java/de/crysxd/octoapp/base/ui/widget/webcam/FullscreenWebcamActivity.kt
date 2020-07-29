package de.crysxd.octoapp.base.ui.widget.webcam

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import de.crysxd.octoapp.base.R


class FullscreenWebcamActivity : AppCompatActivity() {

    companion object {
        fun start(activity: Activity) {
            // For some reason when navigating back after leaving the app, we do not go back to the previous activity
            // so the user left the app accidentally. By using startActivityForResult we prevent this
            activity.startActivityForResult(Intent(activity, FullscreenWebcamActivity::class.java), 23343)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_webcam)

        window.decorView.apply {
            // Hide both the navigation bar and the status bar.
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
}