package de.crysxd.octoapp.remoteaccess

import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.adevinta.android.barista.rule.BaristaRule
import com.adevinta.android.barista.rule.flaky.AllowFlaky
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import de.crysxd.octoapp.MainActivity
import de.crysxd.octoapp.R
import de.crysxd.octoapp.base.di.BaseComponent
import de.crysxd.octoapp.base.di.BaseInjector
import de.crysxd.octoapp.base.usecase.GetRemoteServiceConnectUrlUseCase
import de.crysxd.octoapp.framework.MenuRobot
import de.crysxd.octoapp.framework.TestEnvironmentLibrary
import de.crysxd.octoapp.framework.WorkspaceRobot
import de.crysxd.octoapp.framework.octoeverywhere.OctoEverywhereRobot
import de.crysxd.octoapp.framework.rules.AbstractUseCaseMockRule
import de.crysxd.octoapp.framework.rules.AcceptAllAccessRequestRule
import de.crysxd.octoapp.framework.rules.AutoConnectPrinterRule
import de.crysxd.octoapp.framework.rules.IdleTestEnvironmentRule
import de.crysxd.octoapp.framework.rules.ResetDaggerRule
import de.crysxd.octoapp.framework.rules.TestDocumentationRule
import de.crysxd.octoapp.framework.waitFor
import de.crysxd.octoapp.framework.waitForDialog
import de.crysxd.octoapp.octoprint.exceptions.OctoEverywhereConnectionNotFoundException
import de.crysxd.octoapp.octoprint.exceptions.OctoEverywhereSubscriptionMissingException
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.net.URLEncoder

class OctoEverywhereTest {

    private val testEnv = TestEnvironmentLibrary.Corgi
    private val remoteTestEnv = TestEnvironmentLibrary.CorgiRemote
    private val baristaRule = BaristaRule.create(MainActivity::class.java)

    companion object {
        private const val ID = "connectionid"
        private const val USER = "user"
        private const val PASSWORD = "pw"
        private const val BEARER_TOKEN = "token"
        private const val API_KEY = "api"
    }

    @get:Rule
    val chain = RuleChain.outerRule(baristaRule)
        .around(IdleTestEnvironmentRule(testEnv))
        .around(TestDocumentationRule())
        .around(ResetDaggerRule())
        .around(AcceptAllAccessRequestRule(testEnv))
        .around(MockOctoEverywhereConnectionRule())
        .around(AutoConnectPrinterRule())

    @Test(timeout = 30_000)
    @AllowFlaky(attempts = 3)
    fun WHEN_OctoEverywhere_is_connected_THEN_then_app_can_fall_back() {
        // GIVEN
        OctoEverywhereRobot.setPremiumAccountActive(true)
        BaseInjector.get().octorPrintRepository().setActive(remoteTestEnv)

        // WHEN
        baristaRule.launchActivity()

        // THEN
        waitFor(viewMatcher = allOf(isDisplayed(), withText(R.string.main___banner_connected_via_octoeverywhere)), timeout = 10_000)
        WorkspaceRobot.waitForPrepareWorkspace()
    }

    @Test(timeout = 30_000)
    @AllowFlaky(attempts = 3)
    fun WHEN_OctoEverywhere_is_not_connected_THEN_then_we_can_connect_it() {
        // GIVEN
        OctoEverywhereRobot.setPremiumAccountActive(true)
        BaseInjector.get().octorPrintRepository().setActive(testEnv)
        baristaRule.launchActivity()

        // WHEN
        // Go to connect screen
        WorkspaceRobot.waitForPrepareWorkspace()
        MenuRobot.openMenuWithMoreButton()
        MenuRobot.clickMenuButton(R.string.main_menu___configure_remote_access)

        // Select OE tab
        onView(withText(R.string.configure_remote_access___title)).check(matches(isDisplayed()))
        onView(ViewMatchers.isAssignableFrom(CoordinatorLayout::class.java)).perform(swipeUp())
        onView(withText(R.string.configure_remote_acces___octoeverywhere___title)).perform(click())
        onView(withText(R.string.configure_remote_acces___octoeverywhere___connect_button)).perform(click())

        // THEN
        waitFor(allOf(isDisplayed(), withText(R.string.configure_remote_acces___remote_access_configured)))
        val info = BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()
        assertThat(info).isNotNull()
        assertThat(info!!.alternativeWebUrl).isEqualTo(remoteTestEnv.alternativeWebUrl!!.newBuilder().username(USER).password(PASSWORD).build())
        assertThat(info.octoEverywhereConnection).isNotNull()
        assertThat(info.octoEverywhereConnection!!.apiToken).isEqualTo(API_KEY)
        assertThat(info.octoEverywhereConnection!!.basicAuthPassword).isEqualTo(PASSWORD)
        assertThat(info.octoEverywhereConnection!!.basicAuthUser).isEqualTo(USER)
        assertThat(info.octoEverywhereConnection!!.bearerToken).isEqualTo(BEARER_TOKEN)
        assertThat(info.octoEverywhereConnection!!.connectionId).isEqualTo(ID)

        // WHEN
        onView(withText(R.string.configure_remote_acces___octoeverywhere___disconnect_button)).perform(click())
        waitFor(allOf(isDisplayed(), withText(R.string.configure_remote_acces___remote_access_configured)))
        val info2 = BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()
        assertThat(info2).isNotNull()
        assertThat(info2!!.alternativeWebUrl).isNull()
        assertThat(info2.octoEverywhereConnection).isNull()
    }

    @Test(timeout = 30_000)
    @AllowFlaky(attempts = 3)
    fun WHEN_OctoEverywhere_premium_is_no_longer_available_THEN_then_we_disconnect_it() {
        // GIVEN
        OctoEverywhereRobot.setPremiumAccountActive(false)
        BaseInjector.get().octorPrintRepository().setActive(remoteTestEnv)

        // WHEN
        baristaRule.launchActivity()

        // THEN
        waitForDialog(withText(OctoEverywhereSubscriptionMissingException("http://test.com".toHttpUrl()).userFacingMessage))
        val info = BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()
        assertThat(info).isNotNull()
        assertThat(info!!.alternativeWebUrl).isNull()
        assertThat(info.octoEverywhereConnection).isNull()

        onView(withText(android.R.string.ok)).perform(click())
        waitFor(allOf(isDisplayed(), withText(R.string.connect_printer___octoprint_not_available_title)))
    }

    @Test(timeout = 30_000)
    @AllowFlaky(attempts = 3)
    fun WHEN_OctoEverywhere_connection_was_deleted_THEN_then_we_disconnect_it() {
        // GIVEN
        OctoEverywhereRobot.setPremiumAccountActive(true)
        BaseInjector.get().octorPrintRepository()
            .setActive(remoteTestEnv.copy(alternativeWebUrl = "https://shared-C2WCLVUQYA7EW6HKGAVNWA16DSHPIFV2.octoeverywhere.com".toHttpUrl()))

        // WHEN
        baristaRule.launchActivity()

        // THEN
        waitForDialog(withText(OctoEverywhereConnectionNotFoundException("http://test.com".toHttpUrl()).userFacingMessage))
        val info = BaseInjector.get().octorPrintRepository().getActiveInstanceSnapshot()
        assertThat(info).isNotNull()
        assertThat(info!!.alternativeWebUrl).isNull()
        assertThat(info.octoEverywhereConnection).isNull()

        onView(withText(android.R.string.ok)).perform(click())
        waitFor(allOf(isDisplayed(), withText(R.string.connect_printer___octoprint_not_available_title)))
    }

    inner class MockOctoEverywhereConnectionRule : AbstractUseCaseMockRule() {
        override fun createBaseComponent(base: BaseComponent) = MockBaseComponent(base)

        inner class MockBaseComponent(real: BaseComponent) : BaseComponent by real {
            override fun getConnectOctoEverywhereUrlUseCase(): GetRemoteServiceConnectUrlUseCase = runBlocking {
                val mock: GetRemoteServiceConnectUrlUseCase = mock()
                // We use the octoapp protocol to kick this link directly back to the app in a similar fashion as the web flow. Host must be test.octoapp.eu!
                val url = "octoapp://test.octoapp.eu/${GetRemoteServiceConnectUrlUseCase.OCTOEVERYWHERE_APP_PORTAL_CALLBACK_PATH}?" +
                        "id=$ID&" +
                        "url=${URLEncoder.encode(remoteTestEnv.alternativeWebUrl.toString(), "UTF-8")}&" +
                        "authbasichttpuser=$USER&" +
                        "authbasichttppassword=$PASSWORD&" +
                        "authBearerToken=$BEARER_TOKEN&" +
                        "success=true&" +
                        "appApiToken=$API_KEY&"
                whenever(mock.execute(GetRemoteServiceConnectUrlUseCase.RemoteService.OctoEverywhere))
                    .thenReturn(GetRemoteServiceConnectUrlUseCase.Result.Success(url))

                mock
            }
        }
    }
}