package com.example.pulseworkout

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.example.pulseworkout.update.GithubAsset
import com.example.pulseworkout.update.GithubRelease
import com.example.pulseworkout.update.UpdateState
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Снимки попапа обновления: предложение и загрузка. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class UpdateDialogTest {
    val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(CleanStateRule()).around(rule)

    private val release = GithubRelease(
        tag = "v2.2.0",
        body = "• Проверка обновлений при запуске\n• Исправлен расчёт калорий для кардио\n• Мелкие правки интерфейса",
        pageUrl = "https://github.com/owner/repo/releases/tag/v2.2.0",
        assets = listOf(GithubAsset("Better-Tracking-2.2.0.apk", "https://example.com/app.apk", 3_146_421)),
    )

    @Test
    fun updateDialogStates() {
        val vm = rule.activity.viewModelForTest()
        rule.runOnUiThread { vm.showUpdateForTest(UpdateState.Available(release)) }
        rule.waitForIdle()
        check(rule.onAllNodesWithText("Доступна версия 2.2.0").fetchSemanticsNodes().isNotEmpty())
        captureScreenRoboImage("screenshots/14-update-available.png")
        rule.runOnUiThread { vm.showUpdateForTest(UpdateState.Downloading(release, 0.42f)) }
        rule.waitForIdle()
        captureScreenRoboImage("screenshots/15-update-downloading.png")
    }
}
