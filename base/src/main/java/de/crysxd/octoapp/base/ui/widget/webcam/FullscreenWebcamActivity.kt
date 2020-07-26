package de.crysxd.octoapp.base.ui.widget.webcam

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.crysxd.octoapp.base.R

class FullscreenWebcamActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_webcam)
    }
}