package de.crysxd.baseui.timelapse

import de.crysxd.baseui.BaseViewModel

class TimelapsePlaybackViewModel : BaseViewModel() {
    companion object {
        private const val NO_VALUE = 199
    }

    var playBackPosition = 0L
    var systemUiFlagsBackup: Int? = NO_VALUE
        get() = field.takeIf { it != NO_VALUE }
        set(value) {
            if (field == NO_VALUE) {
                field = value
            }
        }
    var requestedOrientationBackup: Int? = NO_VALUE
        get() = field.takeIf { it != NO_VALUE }
        set(value) {
            if (field == NO_VALUE) {
                field = value
            }
        }
}