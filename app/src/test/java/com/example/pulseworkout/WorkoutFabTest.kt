package com.example.pulseworkout

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.pulseworkout.ui.WorkoutFab
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** FAB тренировки: «Начать» → на экране тренировки «Стоп» → на других экранах «Вернуться» → «Стоп» завершает. */
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class WorkoutFabTest {
    val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(CleanStateRule()).around(rule)

    private val vm get() = rule.activity.viewModelForTest()

    private fun click(text: String) = rule.onAllNodesWithText(text).onLast().performClick().also { rule.waitForIdle() }
    private fun fab(description: String) = rule.onAllNodesWithContentDescription(description).onFirst().performClick().also { rule.waitForIdle() }
    private fun shot(name: String) {
        rule.mainClock.advanceTimeBy(1200)
        rule.waitForIdle()
        captureScreenRoboImage("screenshots/$name.png")
    }

    @Test
    fun fabStates() {
        click("Мужской")
        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("30")
        fields[1].performTextInput("80")
        fields[2].performTextInput("180")
        click("Продолжить")
        click("Сплиты")
        click("Добавить")
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Грудь")
        click("Создать")
        click("Жим штанги лёжа")
        click("Добавить (1)")

        click("Журнал")
        click("Начать тренировку")
        click("Старт")
        assertEquals(WorkoutFab.Stop, vm.workoutFabState())
        shot("30-fab-stop")

        // Другой экран: FAB возвращает к тренировке
        click("Сплиты")
        assertEquals(WorkoutFab.Return, vm.workoutFabState())
        shot("31-fab-return")
        fab("Вернуться к тренировке")
        assertEquals("log", vm.tab)
        assertEquals(WorkoutFab.Stop, vm.workoutFabState())

        // На экране тренировки FAB её завершает
        fab("Остановить тренировку")
        click("Завершить")
        assertNull(vm.session)
        assertEquals(WorkoutFab.Start, vm.workoutFabState())
    }
}
