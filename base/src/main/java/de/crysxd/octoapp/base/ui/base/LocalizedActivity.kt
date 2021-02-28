package de.crysxd.octoapp.base.ui.base

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import de.crysxd.octoapp.base.di.Injector
import kotlinx.coroutines.runBlocking

abstract class LocalizedActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val language = runBlocking {
            Injector.get().getAppLanguageUseCase().execute(Unit)
        }.appLanguageLocale

        super.attachBaseContext(language?.let {
            val config = newBase.resources.configuration
            config.setLocale(it)
            newBase.createConfigurationContext(config)
        } ?: newBase)
    }
}