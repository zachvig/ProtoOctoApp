package de.crysxd.octoapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import timber.log.Timber
import de.crysxd.octoapp.signin.di.Injector as SignInInjector
import de.crysxd.octoapp.connect_printer.di.Injector as ConnectPrinterInjector
import java.lang.RuntimeException

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navController = (supportFragmentManager.findFragmentById(R.id.mainNavController)
                as? NavHostFragment)?.navController

        if (navController != null) {
            SignInInjector.get().octoprintRepository().instanceInformation.observe(
                this,
                Observer {
                    if (it != null) {
                        navController.navigate(R.id.action_sign_in_completed)
                    } else {
                        navController.navigate(R.id.action_sign_in_required)
                    }
                })
            ConnectPrinterInjector.get().octoprintProvider().exception.observe(this, Observer {
                Timber.w("OctoPrint reported error, attempting to reconnect")
                navController.navigate(R.id.action_connect_printer)
            })
        } else {
            throw RuntimeException("mainNavController not available! ")
        }
    }
}