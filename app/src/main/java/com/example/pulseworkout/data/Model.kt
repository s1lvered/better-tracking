package com.example.pulseworkout.data

import kotlinx.serialization.Serializable

/*
 * Модель данных повторяет JSON-структуру `pulse-workout-state-v1` из WebView-версии
 * (app.js), поэтому поля и их названия совпадают с оригиналом.
 * Модель изменяемая: экраны перерисовываются через счётчик ревизий в AppViewModel,
 * так же как в оригинале изменения состояния завершались вызовом render().
 */

@Serializable
data class AppState(
    var catalogVersion: Int = CATALOG_VERSION,
    var profile: Profile = Profile(),
    var profileOnboardingSkipped: Boolean = false,
    var otherName: String = "Прочее",
    var splits: MutableList<Split> = mutableListOf(),
    var exercises: MutableList<Exercise> = mutableListOf(),
    var workouts: MutableList<Workout> = mutableListOf(),
    var cardioHiddenIds: MutableList<String> = mutableListOf(),
    var cardioNames: MutableMap<String, String> = mutableMapOf(),
    // Нет в оригинале: незавершённая тренировка переживает перезапуск приложения.
    var activeSession: ActiveSession? = null,
)

@Serializable
data class Profile(
    var name: String = "Гость",
    var gender: String = "other",
    var weight: Double? = null,
    var age: Int? = null,
    var height: Double? = null,
    var photo: String = "",
    var weights: MutableList<WeightPoint> = mutableListOf(),
)

@Serializable
data class WeightPoint(val date: String, val time: String = "", val weight: Double)

/**
 * archived — сплит «удалён» пользователем: скрыт из списков, но история тренировок
 * по-прежнему показывает его название и цвет (в app.js там было «Без сплита»).
 */
@Serializable
data class Split(val id: String, var name: String, var color: String = "lime", var archived: Boolean = false)

@Serializable
data class Muscles(val primary: List<String> = emptyList(), val secondary: List<String> = emptyList())

@Serializable
data class CalorieModel(val met: Double? = null)

@Serializable
data class Bodyweight(
    val supported: Boolean = false,
    val fraction: Double = 0.0,
    val met: Double? = null,
    val weighted: Boolean = false,
    val defaultWeighted: Boolean = false,
)

@Serializable
data class Exercise(
    val id: String,
    var name: String,
    var category: String = "",
    var muscle: String = "",
    val muscles: Muscles? = null,
    val met: Double? = null,
    val calorieModel: CalorieModel? = null,
    val loadMode: String? = null,
    val baseWeight: Double? = null,
    val bodyweight: Bodyweight? = null,
    var splitIds: MutableList<String> = mutableListOf(),
    var universal: Boolean = false,
    val cardio: Boolean = false,
    // Удалено из библиотеки, но нужно истории (в app.js там было «Удалённое упражнение»).
    var archived: Boolean = false,
)

@Serializable
data class Workout(
    val id: String,
    var date: String,
    var splitId: String,
    var durationSeconds: Long = 0,
    var entries: MutableList<Entry> = mutableListOf(),
)

/** Запись тренировки: силовой подход (type = "strength") или кардио (type = "cardio"). */
@Serializable
data class Entry(
    var type: String = "strength",
    // strength
    var exerciseId: String? = null,
    var instanceId: String? = null,
    var set: Int = 0,
    var weight: Double = 0.0,
    var reps: Int = 0,
    var weighted: Boolean = true,
    var bodyweightMode: Boolean = false,
    var bodyWeight: Double = 0.0,
    // Нет в оригинале: подходы упражнений одного суперсета имеют общий supersetId.
    var supersetId: String? = null,
    // cardio
    var cardioEntryId: String? = null,
    var cardioId: String? = null,
    var cardioName: String? = null,
    var startedAt: Long? = null,
    var endedAt: Long? = null,
    var completed: Boolean? = null,
    var durationSeconds: Long = 0,
    var minutes: Double = 0.0,
    var distance: Double = 0.0,
    var resistance: Double = 0.0,
    var incline: Double = 0.0,
    var speed: Double = 0.0,
    // общее
    var avgHr: Int = 0,
    var hrSamples: MutableList<Int>? = null,
    var calories: Double = 0.0,
) {
    val isCardio get() = type == "cardio"
    val isRunningCardio get() = isCardio && completed == false && startedAt != null
}

/** Упражнение в тренировке; supersetId объединяет упражнения, которые выполняются по кругу (A → B → C). */
@Serializable
data class ExerciseInstance(
    val instanceId: String,
    val exerciseId: String,
    var completed: Boolean = false,
    var supersetId: String? = null,
)

@Serializable
data class ActiveSession(
    var date: String,
    var splitId: String,
    var entries: MutableList<Entry> = mutableListOf(),
    var exerciseInstances: MutableList<ExerciseInstance> = mutableListOf(),
    var hrSamples: MutableList<Int> = mutableListOf(),
    var startedAt: Long? = null,
    var editingWorkoutId: String? = null,
)

const val CATALOG_VERSION = 2
