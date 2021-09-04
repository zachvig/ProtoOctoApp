package de.crysxd.octoapp.base.utils

import android.provider.Settings
import de.crysxd.octoapp.base.di.Injector

object AnimationTestUtils {

    val globalAnimationScale
        get() = Settings.Global.getFloat(
            Injector.get().context().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )

    val animationsDisabled
        get() = globalAnimationScale == 0f
}
