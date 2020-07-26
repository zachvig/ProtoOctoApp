package de.crysxd.octoapp.base.ui.widget.webcam

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import de.crysxd.octoapp.base.R

class FullscreenWebcamActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_webcam)

        window.decorView.apply {
            // Hide both the navigation bar and the status bar.
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
}