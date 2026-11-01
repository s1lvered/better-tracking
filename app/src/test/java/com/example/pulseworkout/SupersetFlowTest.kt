package com.example.pulseworkout

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Суперсет: A + B по кругу, автопереход к B после подхода в A, завершение и статистика. */
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class SupersetFlowTest {
    val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(CleanStateRule()).around(rule)

    private fun shot(name: String) {
        rule.waitForIdle()
        captureScreenRoboImage("screenshots/$name.png")
    }

    private fun click(text: String) = rule.onAllNodesWithText(text).onLast().performClick().also { rule.waitForIdle() }
    private fun clickFirst(text: String) = rule.onAllNodesWithText(text).onFirst().performClick().also { rule.waitForIdle() }
    private fun exists(text: String, substring: Boolean = false) = rule.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun supersetFlow() {
        shot("19-superset-start")
        click("Мужской")
        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("30")
        fields[1].performTextInput("80")
        fields[2].performTextInput("180")
        click("Продолжить")

        // Сплит с двумя упражнениями
        click("Сплиты")
        click("Добавить")
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Грудь")
        click("Создать")
        click("Жим штанги лёжа")
        click("Жим гантелей лёжа")
        click("Добавить (2)")

        click("Журнал")
        click("Начать тренировку")
        click("Старт")
        click("Добавить упражнение")
        click("Жим штанги лёжа")
        click("Добавить подход") // подход 1 в A

        // A → суперсет с B
        click("Суперсет")
        check(exists("Добавить в суперсет")) { "нет шторки суперсета" }
        shot("20-superset-chooser")
        click("Жим гантелей лёжа")
        check(exists("Круг", substring = true)) { "нет карточки суперсета" }

        // Подход в B (последнее упражнение круга) → отдых
        click("Подход")
        check(exists("B · подход 1", substring = true)) { "подход открыт не для B" }
        click("Добавить подход")

        // Подход в A → сразу открывается B
        clickFirst("Подход")
        check(exists("A · подход 2", substring = true)) { "подход открыт не для A" }
        shot("21-superset-set")
        click("Добавить подход")
        check(exists("B · подход 2", substring = true)) { "после A не открылся подход для B" }
        click("Добавить подход")
        shot("22-superset-card")

        // Завершить суперсет → свёрнутая карточка
        click("Завершить")
        click("Завершить") // подтверждение
        check(exists("Изменить")) { "суперсет не свернулся" }
        shot("23-superset-done")

        click("Завершить тренировку")
        click("Завершить")
        check(exists("Суперсет")) { "в статистике нет суперсета" }
        shot("24-superset-stats")
    }
}
