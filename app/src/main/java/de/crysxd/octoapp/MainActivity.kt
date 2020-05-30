package de.crysxd.octoapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import de.crysxd.octoapp.signin.di.Injector

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.init(application)
        setContentView(R.layout.activity_main)


    }
}