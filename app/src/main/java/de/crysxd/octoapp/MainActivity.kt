package de.crysxd.octoapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import de.crysxd.octoapp.signin.di.Injector
import java.lang.RuntimeException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.mainNavController)
                as? NavHostFragment

        if (navHostFragment != null) {
            Injector.get().octoprintRepository().instanceInforationAvailable.observe(
                this,
                Observer {
                    if (it) {
                        navHostFragment.navController.navigate(R.id.action_sign_in_completed)
                    } else {
                        navHostFragment.navController.navigate(R.id.action_sign_in_required)
                    }
                })
        } else {
            throw RuntimeException("mainNavController not available! ")
        }
    }
}