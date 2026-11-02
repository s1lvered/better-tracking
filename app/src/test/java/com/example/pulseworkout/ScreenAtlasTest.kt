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
import com.example.pulseworkout.data.Entry
import com.example.pulseworkout.data.WeightPoint
import com.example.pulseworkout.data.Workout
import com.example.pulseworkout.logic.Calc
import com.example.pulseworkout.ui.AppViewModel
import com.example.pulseworkout.ui.Sheet
import com.example.pulseworkout.update.GithubAsset
import com.example.pulseworkout.update.GithubRelease
import com.example.pulseworkout.update.UpdateState
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Атлас экранов для Figma: проходит по всем экранам и состояниям приложения на демо-данных
 * и сохраняет снимки в `screenshots/atlas/` (префикс — ветка user flow: A1, B2…).
 */
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenAtlasTest {
    val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(CleanStateRule()).around(rule)

    private lateinit var vm: AppViewModel

    // Тосты скрываем (ждём, пока исчезнут), кроме снимков, где тост — часть состояния.
    private fun shot(name: String, keepToast: Boolean = false) {
        rule.waitForIdle()
        if (!keepToast) rule.mainClock.advanceTimeBy(3000)
        rule.waitForIdle()
        captureScreenRoboImage("screenshots/atlas/$name.png")
    }

    private fun click(text: String) = rule.onAllNodesWithText(text).onLast().performClick().also { rule.waitForIdle() }
    private fun ui(block: AppViewModel.() -> Unit) = rule.runOnUiThread { vm.block() }.also { rule.waitForIdle() }
    private fun ex(name: String) = vm.state.exercises.first { it.name == name }.id

    @Test
    fun atlas() {
        vm = rule.activity.viewModelForTest()

        // ---------- A. Онбординг ----------
        shot("A1-onboarding")
        click("Мужской")
        val fields = rule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("29")
        fields[1].performTextInput("82")
        fields[2].performTextInput("181")
        fields[3].performTextInput("Алекс")
        shot("A2-onboarding-filled")
        click("Продолжить")
        shot("A3-journal-first-run")

        // ---------- B. Сплиты ----------
        click("Сплиты")
        shot("B1-splits-empty")
        click("Добавить")
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Грудь")
        shot("B2-split-new")
        click("Создать")
        shot("B3-split-picker")
        click("Жим штанги лёжа")
        click("Жим гантелей лёжа")
        click("Жим штанги на наклонной скамье")
        shot("B4-split-picker-selected")
        click("Добавить (3)")
        seed()
        shot("B5-splits")
        ui { deleteSplit(state.splits.last().id) }
        shot("B6-split-delete")
        ui { confirm = null }

        // ---------- C. Библиотека ----------
        click("Упражнения")
        shot("C1-library")
        ui { exerciseQuery = "жим" }
        shot("C2-library-search")
        ui { exerciseQuery = "кроссфит" }
        shot("C3-library-not-found")
        ui { exerciseQuery = ""; exerciseAreaTab = com.example.pulseworkout.data.Catalog.categoryGroups[1].id }
        shot("C4-library-area")
        ui { exerciseAreaTab = "all"; sheet = Sheet.ExerciseEdit(null) }
        shot("C5-exercise-new")
        ui { sheet = null; deleteExercise(ex("Жим штанги лёжа")) }
        shot("C6-exercise-delete")
        ui { confirm = null }

        // ---------- D. Журнал и история ----------
        click("Журнал")
        shot("D1-journal")
        click("за неделю")
        shot("D2-journal-period")
        click("за месяц")
        shot("D3-journal-month")
        val supersetWorkout = vm.state.workouts.first { w -> w.entries.any { it.supersetId != null } }.id
        ui { sheet = Sheet.WorkoutStats(supersetWorkout) }
        shot("D4-workout-details")
        rule.onAllNodesWithContentDescription("Действия с тренировкой").onLast().performClick()
        shot("D5-workout-actions")
        ui { sheet = null; deleteWorkout(supersetWorkout) }
        shot("D6-workout-delete")
        ui { confirm = null; openWorkoutEditor(supersetWorkout) }
        shot("D7-workout-edit")
        ui { sheet = Sheet.CardioManual(calc.visibleCardioOptions().first().id) }
        shot("D8-cardio-manual")
        ui { sheet = null; requestFinishSession() }
        shot("D9-edit-save")
        click("Сохранить")
        ui { journalPeriod = "week" }

        // ---------- E. Тренировка ----------
        ui { requestStartSession() }
        shot("E1-start-workout")
        click("Старт")
        ui { session!!.startedAt = System.currentTimeMillis() - (24 * 60 + 18) * 1000L; changed(persist = false) }
        shot("E2-session-empty")
        click("Добавить упражнение")
        shot("E3-exercise-chooser")
        click("Кардио")
        shot("E4-chooser-cardio")
        ui { closeSheet(); selectSessionExercise(ex("Жим штанги лёжа")) }
        shot("E5-set-entry")
        val bench = vm.session!!.exerciseInstances.first().instanceId
        ui {
            addSet(ex("Жим штанги лёжа"), bench, 60.0, 10, true)
            addSet(ex("Жим штанги лёжа"), bench, 70.0, 8, true)
            addSet(ex("Жим штанги лёжа"), bench, 80.0, 6, true)
        }
        shot("E6-session-sets")
        ui { sheet = Sheet.Wearable }
        shot("E7-wearable")
        ui { closeSheet(); finishExercise(bench) }
        shot("E8-exercise-finish")
        click("Завершить")
        shot("E9-exercise-done")

        // ---------- F. Суперсет ----------
        ui { selectSessionExercise(ex("Жим штанги на наклонной скамье")) }
        val incline = vm.session!!.exerciseInstances.first().instanceId
        ui { addSet(ex("Жим штанги на наклонной скамье"), incline, 50.0, 10, true) }
        ui { startSuperset(incline) }
        shot("F1-superset-chooser")
        ui { selectSessionExercise(ex("Жим гантелей лёжа"), incline) }
        shot("F2-superset-created")
        val dumbbell = vm.session!!.exerciseInstances.first { it.exerciseId == ex("Жим гантелей лёжа") }.instanceId
        ui { addSet(ex("Жим гантелей лёжа"), dumbbell, 24.0, 10, true) }
        ui { addSet(ex("Жим штанги на наклонной скамье"), incline, 55.0, 8, true) }
        shot("F3-superset-set")
        ui {
            addSet(ex("Жим гантелей лёжа"), dumbbell, 26.0, 8, true)
        }
        shot("F4-superset-card")
        ui { finishSuperset(session!!.exerciseInstances.first { it.instanceId == incline }.supersetId!!) }
        shot("F5-superset-finish")
        click("Завершить")
        shot("F6-superset-done")

        // ---------- G. Кардио и завершение ----------
        ui {
            startCardio(calc.visibleCardioOptions().first().id)
            session!!.entries.first { it.isRunningCardio }.startedAt = System.currentTimeMillis() - 12 * 60 * 1000L
            changed(persist = false)
        }
        shot("G1-cardio-running")
        ui { tab = "journal" }
        shot("G2-journal-live")
        ui { tab = "log"; openCardioFinish(session!!.entries.first { it.isRunningCardio }.cardioEntryId!!) }
        shot("G3-cardio-finish")
        click("Завершить кардио")
        ui { requestFinishSession() }
        shot("G4-workout-finish")
        click("Завершить")
        shot("G5-workout-saved")
        click("В журнал")
        shot("G6-journal-after")

        // ---------- H. Профиль ----------
        ui { sheet = Sheet.Profile }
        shot("H1-profile")
        ui { sheet = Sheet.Weighing }
        shot("H2-weighing")
        ui { addWeighing(81.6) }
        shot("H3-weight-saved", keepToast = true)
        ui { sheet = Sheet.ProfileEdit }
        shot("H4-profile-edit")
        ui { sheet = null }

        // ---------- I. Обновление ----------
        val release = GithubRelease(
            tag = "v2.4.0",
            body = "• Суперсеты в истории тренировок\n• Новый экран профиля\n• Исправления и ускорение",
            pageUrl = "https://github.com/s1lvered/better-tracking/releases/tag/v2.4.0",
            assets = listOf(GithubAsset("Better-Tracking-2.4.0.apk", "https://example.com/app.apk", 3_163_478)),
        )
        ui { showUpdateForTest(UpdateState.Available(release)) }
        shot("I1-update-available")
        ui { showUpdateForTest(UpdateState.Downloading(release, 0.42f)) }
        shot("I2-update-downloading")
        ui { showUpdateForTest(UpdateState.Ready(release, File("update.apk"))) }
        shot("I3-update-ready")
        ui { showUpdateForTest(UpdateState.Failed(release, "Не удалось скачать обновление. Проверьте интернет и попробуйте ещё раз.")) }
        shot("I4-update-failed")
    }

    /** Демо-данные: три сплита, история за месяц (одна тренировка с суперсетом), динамика веса. */
    private fun seed() = ui {
        saveSplit(null, "Спина")
        addExercisesToSplit(state.splits.last().id, setOf(ex("Подтягивания прямым хватом"), ex("Тяга штанги в наклоне"), ex("Тяга верхнего блока широким хватом")))
        saveSplit(null, "Ноги")
        addExercisesToSplit(state.splits.last().id, setOf(ex("Присед со штангой на спине"), ex("Разгибание ног в тренажёре"), ex("Становая тяга классическая")))
        expandedSplits.clear()
        expandedSplits += state.splits.first().id

        val plans = listOf(
            "Грудь" to listOf("Жим штанги лёжа" to 70.0, "Жим гантелей лёжа" to 24.0, "Жим штанги на наклонной скамье" to 50.0),
            "Спина" to listOf("Тяга штанги в наклоне" to 60.0, "Тяга верхнего блока широким хватом" to 55.0, "Подтягивания прямым хватом" to 0.0),
            "Ноги" to listOf("Присед со штангой на спине" to 90.0, "Разгибание ног в тренажёре" to 40.0, "Становая тяга классическая" to 100.0),
        )
        val today = LocalDate.now()
        listOf(26, 23, 21, 18, 16, 13, 11, 8, 6, 4, 2, 1).forEachIndexed { i, daysAgo ->
            val (splitName, list) = plans[i % 3]
            val split = state.splits.first { it.name == splitName }
            val superset = i == 10 // тренировка груди позавчера — с суперсетом
            val groupId = if (superset) "superset-demo" else null
            val entries = mutableListOf<Entry>()
            list.forEachIndexed { k, (name, base) ->
                val inGroup = superset && k < 2
                repeat(if (inGroup) 3 else 4) { set ->
                    entries += Entry(
                        exerciseId = ex(name), instanceId = "demo-$i-$k", set = set,
                        weight = base + i * 1.25 + set * 2.5, reps = 10 - set, weighted = base > 0,
                        bodyweightMode = base == 0.0, bodyWeight = 82.0,
                        supersetId = if (inGroup) groupId else null,
                    ).also { it.calories = calc.estimateCalories(it).roundToInt().toDouble() }
                }
            }
            if (i % 4 == 1) {
                val cardio = calc.visibleCardioOptions().first()
                entries += Entry(type = "cardio", cardioEntryId = "demo-cardio-$i", cardioId = cardio.id, cardioName = cardio.name, completed = true, durationSeconds = 900, minutes = 15.0, distance = 2.4, avgHr = 138, calories = 140.0)
            }
            state.workouts += Workout("demo-w-$i", today.minusDays(daysAgo.toLong()).toString(), split.id, (52 + i * 3) * 60L, entries)
        }
        // Динамика веса за два месяца
        state.profile.weights.clear()
        listOf(60 to 84.2, 50 to 83.8, 42 to 83.5, 35 to 83.1, 28 to 82.9, 21 to 82.6, 14 to 82.4, 7 to 82.1, 0 to 82.0).forEach { (d, w) ->
            state.profile.weights += WeightPoint(today.minusDays(d.toLong()).toString(), "08:30", w)
        }
        closeSheet()
        changed()
    }
}
