package com.example.pulseworkout

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Сквозной сценарий: профиль → сплит → тренировка → подходы → журнал, со снимками экранов. */
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppFlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    // Снимок всего экрана, включая шторки и диалоги (они живут в отдельных окнах).
    private fun shot(name: String) {
        rule.waitForIdle()
        captureScreenRoboImage("screenshots/$name.png")
    }

    // Последнее совпадение: шторки и диалоги компонуются поверх основного экрана.
    private fun click(text: String) = rule.onAllNodesWithText(text).onLast().performClick().also { rule.waitForIdle() }

    private fun count(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun fullWorkoutFlow() {
        shot("01-onboarding")
        click("Мужской")
        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("30")
        fields[1].performTextInput("80")
        fields[2].performTextInput("180")
        fields[3].performTextInput("Тест")
        click("Продолжить")
        shot("02-journal-empty")

        click("Сплиты")
        click("Добавить")
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Грудь")
        click("Создать")
        shot("03-split-picker")
        click("Жим штанги лёжа")
        click("Добавить (1)")
        shot("04-splits")

        click("Упражнения")
        shot("05-library")

        click("Журнал")
        click("Начать тренировку")
        shot("06-start-sheet")
        click("Старт")
        shot("07-session")
        click("Добавить упражнение")
        click("Жим штанги лёжа")
        shot("08-set-entry")
        click("Добавить подход")
        click("Подход")
        click("Добавить подход")
        check(count("x1") == 2) { "ожидалось 2 подхода" }
        shot("09-session-with-sets")

        // Удаление ошибочного подхода
        rule.onAllNodesWithContentDescription("Удалить подход").onLast().performClick()
        rule.waitForIdle()
        check(count("x1") == 1) { "подход не удалён" }

        // Журнал во время тренировки показывает «живую» карточку, по ней возвращаемся
        click("Журнал")
        shot("10-journal-live")
        click("Активная тренировка")

        click("Завершить тренировку")
        click("Завершить") // кнопка подтверждения в диалоге
        shot("11-workout-stats")
        click("В журнал")
        check(rule.onAllNodes(hasText("ккал", substring = true)).fetchSemanticsNodes().isNotEmpty())
        check(count("Грудь") >= 1 && count("Активная тренировка") == 0) { "тренировка не попала в журнал" }
        shot("12-journal")

        click("Тест")
        shot("13-profile")

        click("Новое взвешивание")
        shot("16-weighing")
        click("Сохранить")
        check(count("Вес сохранён") == 1) { "нет тоста о сохранении веса" }
        click("Изменить")
        shot("17-profile-edit")
    }
}
