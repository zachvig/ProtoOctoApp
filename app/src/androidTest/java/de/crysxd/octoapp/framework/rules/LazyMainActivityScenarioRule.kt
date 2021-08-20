package de.crysxd.octoapp.framework.rules

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import de.crysxd.octoapp.MainActivity

class LazyMainActivityScenarioRule : LazyActivityScenarioRule<MainActivity>(
    launchActivity = false,
    startActivityIntent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, MainActivity::class.java)
)
