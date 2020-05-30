package de.crysxd.octoapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import de.crysxd.octoapp.signin.di.Injector
import java.lang.RuntimeException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.init(application)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.mainNavController) as? NavHostFragment
        if (navHostFragment != null) {
            val signedIn = Injector.get().octoprintRepository().isInstanceInformationAvailable()
            if (signedIn) {
                navHostFragment.navController.navigate(R.id.action_sign_in_completed)
            } else {
                navHostFragment.navController.navigate(R.id.action_sign_in_required)
            }
        } else {
            throw RuntimeException("mainNavController not available! ")
        }
    }
}