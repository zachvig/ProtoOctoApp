package de.crysxd.octoapp.base.ui.menu.webcam

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import de.crysxd.octoapp.base.R
import de.crysxd.octoapp.base.UriLibrary
import de.crysxd.octoapp.base.billing.BillingManager
import de.crysxd.octoapp.base.di.Injector
import de.crysxd.octoapp.base.ext.open
import de.crysxd.octoapp.base.ui.ext.requireOctoActivity
import de.crysxd.octoapp.base.ui.menu.*
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_ENABLE_FULL_WEBCAM_RESOLUTION
import de.crysxd.octoapp.base.ui.menu.main.MENU_ITEM_SHOW_WEBCAM_RESOLUTION
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt

@Parcelize
class WebcamSettingsMenu : Menu {
    override suspend fun getMenuItem() = listOf(
        ShowResolutionMenuItem(),
        EnableFullResolutionMenuItem()
    )

    override suspend fun getTitle(context: Context) = context.getString(R.string.webcam_settings___title)
    override suspend fun getSubtitle(context: Context) = context.getString(R.string.webcam_settings___subtitle)

    class ShowResolutionMenuItem : ToggleMenuItem() {
        override val isEnabled get() = Injector.get().octoPreferences().isShowWebcamResolution

        override val itemId = MENU_ITEM_SHOW_WEBCAM_RESOLUTION
        override var groupId = ""
        override val order = 350
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_image_aspect_ratio_24

        override suspend fun getTitle(context: Context) = context.getString(R.string.webcam_settings___show_resolution)

        override suspend fun handleToggleFlipped(host: MenuBottomSheetFragment, enabled: Boolean) {
            Injector.get().octoPreferences().isShowWebcamResolution = enabled
        }
    }

    class EnableFullResolutionMenuItem : MenuItem {
        private val maxResolution = (FirebaseRemoteConfig.getInstance().getLong("free_webcam_max_resolution") * (1080 / 1920f)).roundToInt()
        override val itemId = MENU_ITEM_ENABLE_FULL_WEBCAM_RESOLUTION
        override var groupId = ""
        override val order = 351
        override val style = MenuItemStyle.Settings
        override val icon = R.drawable.ic_round_photo_size_select_large_24
        override suspend fun isVisible(destinationId: Int) = !BillingManager.isFeatureEnabled(BillingManager.FEATURE_FULL_WEBCAM_RESOLUTION)

        override suspend fun getTitle(context: Context) = context.getString(R.string.webcam_settings___enable_full_resolution)
        override suspend fun getDescription(context: Context) = context.getString(R.string.webcam_settings___enable_full_resolution_explainer, maxResolution)

        override suspend fun onClicked(host: MenuBottomSheetFragment?) {
            host?.requireOctoActivity()?.let {
                UriLibrary.getPurchaseUri().open(it)
            }
        }
    }
}