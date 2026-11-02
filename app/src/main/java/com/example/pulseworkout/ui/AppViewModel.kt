package com.example.pulseworkout.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pulseworkout.BuildConfig
import com.example.pulseworkout.update.UpdateState
import com.example.pulseworkout.update.Updater
import com.example.pulseworkout.ble.HeartRateManager
import com.example.pulseworkout.data.ActiveSession
import com.example.pulseworkout.data.AppState
import com.example.pulseworkout.data.Catalog
import com.example.pulseworkout.data.Entry
import com.example.pulseworkout.data.Exercise
import com.example.pulseworkout.data.ExerciseInstance
import com.example.pulseworkout.data.Repository
import com.example.pulseworkout.data.Split
import com.example.pulseworkout.data.WeightPoint
import com.example.pulseworkout.data.Workout
import com.example.pulseworkout.logic.Calc
import com.example.pulseworkout.logic.Calc.Companion.plural
import com.example.pulseworkout.logic.Calc.Companion.today
import com.example.pulseworkout.logic.Calc.Companion.uid
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/** Состояния FAB тренировки: начать, вернуться к идущей, остановить (на её экране). */
enum class WorkoutFab { Start, Return, Stop }

sealed interface Sheet {
    data object StartWorkout : Sheet
    /** supersetWith — instanceId упражнения, к которому добавляется второе упражнение суперсета. */
    data class ExerciseChooser(val supersetWith: String? = null) : Sheet
    data class SetEntry(val exerciseId: String, val instanceId: String) : Sheet
    data class CardioFinish(val cardioEntryId: String, val requestedAt: Long) : Sheet
    /** Кардио в уже прошедшей (редактируемой) тренировке вводится вручную, без живого таймера. */
    data class CardioManual(val activityId: String) : Sheet
    data class WorkoutStats(val workoutId: String, val completion: Boolean = false) : Sheet
    data object Profile : Sheet
    data object ProfileEdit : Sheet
    data object Weighing : Sheet
    data class ExerciseEdit(val exerciseId: String?) : Sheet
    data class SplitEdit(val splitId: String?) : Sheet
    data class SplitPicker(val splitId: String) : Sheet
    data object Wearable : Sheet
}

data class ConfirmRequest(val title: String, val message: String, val confirmLabel: String, val onConfirm: () -> Unit)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    val feedback = Feedback(app)

    var state: AppState = repo.load()
        private set

    /** Счётчик ревизий: любое изменение модели увеличивает его и перерисовывает экраны (аналог render()). */
    var revision by mutableIntStateOf(0)
        private set

    val calc = Calc { state }

    /** id упражнений встроенного каталога (их категория фиксирована). */
    val catalogIds: Set<String> by lazy { repo.catalog.map { it.id }.toSet() }

    var tab by mutableStateOf("journal")
    var journalPeriod by mutableStateOf("week")
    var exerciseQuery by mutableStateOf("")
    var exerciseAreaTab by mutableStateOf("all")
    var profileChartPeriod by mutableStateOf("month")
    val expandedSplits = mutableStateListOf<String>()
    val splitCategoryTabs = mutableStateMapOf<String, String>()

    var sheet by mutableStateOf<Sheet?>(null)
    var confirm by mutableStateOf<ConfirmRequest?>(null)
    var toast by mutableStateOf<Pair<String, Long>?>(null)
        private set

    /** Текущее время для таймеров, обновляется раз в секунду. */
    var now by mutableLongStateOf(System.currentTimeMillis())
        private set

    // Секундомер отдыха
    var restStartedAt by mutableLongStateOf(0L)
        private set
    private var restElapsed = 0L
    private var restLastCueMinute = 0L

    private val pendingSetHrSamples = mutableListOf<Int>()

    val wearable = HeartRateManager(app, ::onHeartRate)

    private var ticker: Job? = null

    val session: ActiveSession? get() = state.activeSession

    // ---------- обновления ----------
    val updater = Updater(app, BuildConfig.UPDATE_REPO, BuildConfig.VERSION_NAME)

    /** Попап обновления: null — нечего показывать. */
    var update by mutableStateOf<UpdateState?>(null)
        private set
    private var downloadJob: Job? = null

    init {
        // Фоновая проверка последнего релиза при запуске; без сети просто ничего не показываем.
        if (updater.enabled) viewModelScope.launch {
            updater.check()?.let { update = UpdateState.Available(it) }
        }
        // Тост «Браслет подключён», как в макете «Wearable / 5»
        viewModelScope.launch {
            var wasConnected = false
            wearable.state.collect { s ->
                val connected = s.connected != null
                if (connected && !wasConnected) showToast("Браслет подключён")
                wasConnected = connected
            }
        }
    }

    fun downloadUpdate() {
        val release = when (val u = update) {
            is UpdateState.Available -> u.release
            is UpdateState.Failed -> u.release
            else -> return
        }
        val asset = release.apk ?: run { updater.openPage(release); update = null; return }
        update = UpdateState.Downloading(release, 0f)
        downloadJob = viewModelScope.launch {
            update = try {
                val file = updater.download(asset) { p -> update = UpdateState.Downloading(release, p) }
                UpdateState.Ready(release, file)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                UpdateState.Failed(release, "Не удалось скачать обновление. Проверьте интернет и попробуйте ещё раз.")
            }
            (update as? UpdateState.Ready)?.let { installUpdate() }
        }
    }

    fun installUpdate() {
        val ready = update as? UpdateState.Ready ?: return
        if (updater.needsInstallPermission()) {
            showToast("Разрешите установку из Better Tracking и нажмите «Установить» ещё раз")
            runCatching { getApplication<Application>().startActivity(updater.installPermissionIntent()) }
            return
        }
        runCatching { updater.install(ready.file) }.onFailure { update = UpdateState.Failed(ready.release, "Не удалось открыть установщик") }
    }

    /** Только для UI-тестов: показать попап без обращения к сети. */
    @androidx.annotation.VisibleForTesting
    fun showUpdateForTest(state: UpdateState) {
        update = state
    }

    fun dismissUpdate() {
        downloadJob?.cancel()
        downloadJob = null
        update = null
    }

    init {
        if (session?.editingWorkoutId == null && session != null) startTicker()
        // Отредактированная тренировка не должна «зависнуть» после перезапуска.
        if (session?.editingWorkoutId != null) state.activeSession = null
        if (session != null) tab = "log"
    }

    override fun onCleared() {
        wearable.release()
        super.onCleared()
    }

    // ---------- общее ----------

    fun changed(persist: Boolean = true) {
        if (persist) runCatching { repo.save(state) }
        revision++
    }

    fun showToast(message: String) {
        toast = message to System.currentTimeMillis()
    }

    fun ask(title: String, message: String, label: String, action: () -> Unit) {
        confirm = ConfirmRequest(title, message, label, action)
    }

    fun closeSheet() {
        if (sheet is Sheet.SetEntry) pendingSetHrSamples.clear()
        sheet = null
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = viewModelScope.launch {
            while (true) {
                now = System.currentTimeMillis()
                checkRestCue()
                delay(1000L - now % 1000L)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    // ---------- секундомер отдыха ----------

    fun restElapsedSeconds(): Long =
        if (restStartedAt > 0) max(0L, (now - restStartedAt) / 1000) else restElapsed

    fun startRest() {
        restStartedAt = System.currentTimeMillis()
        restElapsed = 0
        restLastCueMinute = 0
        now = restStartedAt
        startTicker()
    }

    fun stopRest() {
        restElapsed = restElapsedSeconds()
        restStartedAt = 0
    }

    private fun resetRest() {
        restStartedAt = 0
        restElapsed = 0
        restLastCueMinute = 0
    }

    private fun checkRestCue() {
        if (restStartedAt == 0L) return
        val minute = restElapsedSeconds() / 60
        if (minute > 0 && minute > restLastCueMinute) {
            restLastCueMinute = minute
            feedback.restMinuteCue()
        }
    }

    fun sessionElapsedSeconds(): Long = session?.startedAt?.let { max(0L, (now - it) / 1000) } ?: 0L

    // ---------- пульс ----------

    private fun onHeartRate(bpm: Int) {
        val s = session
        val setActive = sheet is Sheet.SetEntry
        val cardioActive = s?.entries?.any { it.isRunningCardio } == true
        if (setActive) {
            pendingSetHrSamples += bpm
            if (pendingSetHrSamples.size > 1800) pendingSetHrSamples.removeAt(0)
        }
        if (s != null && (setActive || cardioActive)) {
            s.hrSamples += bpm
            if (s.hrSamples.size > 10800) s.hrSamples.removeAt(0)
        }
        s?.entries?.filter { it.isRunningCardio }?.forEach { entry ->
            val samples = entry.hrSamples ?: mutableListOf<Int>().also { entry.hrSamples = it }
            samples += bpm
            if (samples.size > 10800) samples.removeAt(0)
        }
        if (s != null && (setActive || cardioActive)) changed(persist = false)
    }

    fun sessionAverageHr(): Int? = session?.hrSamples?.takeIf { it.isNotEmpty() }?.average()?.roundToInt()

    fun toggleWearable() {
        if (wearable.state.value.connected != null) wearable.disconnect() else sheet = Sheet.Wearable
    }

    private fun disconnectWearableAfterWorkout() {
        val w = wearable.state.value
        if (w.connected != null || w.status == "connecting") wearable.disconnect()
    }

    // ---------- старт / финиш тренировки ----------

    /** Состояние FAB: активна только идущая (не редактируемая) тренировка; «Стоп» — когда открыт её экран. */
    fun workoutFabState(): WorkoutFab {
        val s = session
        return when {
            s == null || s.editingWorkoutId != null -> WorkoutFab.Start
            tab == "log" -> WorkoutFab.Stop
            else -> WorkoutFab.Return
        }
    }

    /** FAB: запускает тренировку, возвращает к идущей или (на её экране) завершает её. */
    fun onWorkoutButton() {
        when (workoutFabState()) {
            WorkoutFab.Start -> requestStartSession()
            WorkoutFab.Return -> tab = "log"
            WorkoutFab.Stop -> requestFinishSession()
        }
    }

    fun requestStartSession() {
        val s = session
        if (s == null) { sheet = Sheet.StartWorkout; return }
        if (s.editingWorkoutId != null) {
            ask("Начать новую тренировку?", "Текущее редактирование будет сохранено, после чего можно будет выбрать новый сплит.", "Сохранить и начать") {
                completeWorkoutEdit()
                sheet = Sheet.StartWorkout
            }
            return
        }
        val unfinished = s.exerciseInstances.filter { !it.completed }
        val description = if (s.entries.isNotEmpty())
            "${if (unfinished.isNotEmpty()) "Незавершённое упражнение тоже будет завершено. " else ""}Текущая тренировка сохранится в журнале, затем начнётся новая."
        else "Текущая пустая тренировка будет завершена без записи в журнал, затем начнётся новая."
        ask("Завершить текущую тренировку?", description, "Завершить и начать") {
            unfinished.forEach { it.completed = true }
            if (s.entries.isNotEmpty()) saveSessionAsWorkout(s)
            endSession()
            tab = "journal"
            changed()
            sheet = Sheet.StartWorkout
        }
    }

    fun startSession(splitId: String) {
        state.activeSession = ActiveSession(date = today(), splitId = splitId, startedAt = System.currentTimeMillis())
        sheet = null
        tab = "log"
        now = System.currentTimeMillis()
        startTicker()
        changed()
    }

    private fun saveSessionAsWorkout(s: ActiveSession): Workout {
        val workout = Workout(
            id = uid("w"),
            date = s.date,
            splitId = s.splitId,
            durationSeconds = s.startedAt?.let { max(0L, (System.currentTimeMillis() - it) / 1000) } ?: 0L,
            entries = s.entries.map { it.copy(calories = if (it.calories > 0) it.calories else calc.estimateCalories(it).roundToInt().toDouble()) }.toMutableList(),
        )
        state.workouts += workout
        return workout
    }

    private fun endSession() {
        state.activeSession = null
        stopTicker()
        resetRest()
        disconnectWearableAfterWorkout()
    }

    fun requestFinishSession() {
        val s = session ?: return
        s.entries.firstOrNull { it.isRunningCardio }?.let { running ->
            showToast("Сначала завершите текущее кардио")
            openCardioFinish(running.cardioEntryId!!)
            return
        }
        if (s.editingWorkoutId != null) {
            ask("Завершить редактирование?", "Изменения тренировки будут сохранены в журнале.", "Сохранить") { completeWorkoutEdit() }
            return
        }
        if (s.entries.isEmpty()) {
            ask("Завершить пустую тренировку?", "Упражнения не были выполнены, поэтому тренировка не будет записана в журнал.", "Завершить") {
                endSession()
                tab = "journal"
                changed()
                showToast("Пустая тренировка не записана")
            }
            return
        }
        val unfinished = s.exerciseInstances.filter { !it.completed }
        val names = unfinished.joinToString(", ") { calc.exercise(it.exerciseId).name }
        val description = if (unfinished.isNotEmpty())
            "Незавершённые упражнения ($names) будут завершены автоматически. После этого сохранится вся тренировка."
        else "Все упражнения уже завершены. Тренировка будет сохранена в журнале."
        ask(if (unfinished.isNotEmpty()) "Завершить упражнение и тренировку?" else "Завершить тренировку?", description, "Завершить") {
            unfinished.forEach { it.completed = true }
            val workout = saveSessionAsWorkout(s)
            endSession()
            tab = "journal"
            changed()
            sheet = Sheet.WorkoutStats(workout.id, completion = true)
        }
    }

    private fun completeWorkoutEdit() {
        val s = session ?: return
        val workout = state.workouts.firstOrNull { it.id == s.editingWorkoutId } ?: return
        workout.date = s.date
        workout.splitId = s.splitId
        workout.entries = s.entries.map { it.copy(calories = if (it.calories > 0) it.calories else calc.estimateCalories(it).roundToInt().toDouble()) }.toMutableList()
        state.activeSession = null
        stopTicker()
        tab = "journal"
        changed()
        showToast("Тренировка обновлена")
    }

    fun openWorkoutEditor(id: String) {
        val workout = state.workouts.firstOrNull { it.id == id } ?: return
        val entries = workout.entries.map { it.copy(hrSamples = it.hrSamples?.toMutableList()) }.toMutableList()
        val instances = mutableListOf<ExerciseInstance>()
        val byKey = mutableMapOf<String, ExerciseInstance>()
        entries.filter { !it.isCardio && it.exerciseId != null }.forEach { entry ->
            val key = entry.instanceId ?: "exercise:${entry.exerciseId}"
            val instance = byKey.getOrPut(key) {
                ExerciseInstance(uid("edit-exercise"), entry.exerciseId!!, completed = true, supersetId = entry.supersetId).also { instances += it }
            }
            entry.instanceId = instance.instanceId
        }
        state.activeSession = ActiveSession(date = workout.date, splitId = workout.splitId, entries = entries, exerciseInstances = instances, editingWorkoutId = workout.id)
        stopTicker()
        resetRest()
        sheet = null
        tab = "log"
        changed(persist = false)
    }

    fun deleteWorkout(id: String) {
        val workout = state.workouts.firstOrNull { it.id == id } ?: return
        val split = calc.split(workout.splitId)
        ask("Удалить тренировку?", "«${split.name}» от ${Calc.journalDateLabel(workout.date).lowercase()} и все её подходы будут удалены.", "Удалить") {
            state.workouts.removeAll { it.id == id }
            changed()
            showToast("Тренировка удалена")
        }
    }

    // ---------- упражнения в тренировке ----------

    fun instanceEntries(instance: ExerciseInstance): List<Entry> = session?.entries.orEmpty().filter {
        !it.isCardio && (if (it.instanceId != null) it.instanceId == instance.instanceId else it.exerciseId == instance.exerciseId)
    }

    private fun addSessionExercise(exerciseId: String): ExerciseInstance {
        val instances = session!!.exerciseInstances
        instances.firstOrNull { it.exerciseId == exerciseId }?.let { existing ->
            existing.completed = false
            instances.remove(existing)
            instances.add(0, existing)
            return existing
        }
        return ExerciseInstance(uid("exercise"), exerciseId).also { instances.add(0, it) }
    }

    // ---------- суперсеты ----------

    /** Упражнения суперсета в порядке выполнения (A, B, C — в порядке добавления). */
    fun supersetMembers(supersetId: String): List<ExerciseInstance> =
        session?.exerciseInstances.orEmpty().filter { it.supersetId == supersetId }.reversed()

    /** Буква упражнения в суперсете: A, B, C… */
    fun supersetLetter(instance: ExerciseInstance): String {
        val id = instance.supersetId ?: return ""
        return ('A' + supersetMembers(id).indexOf(instance).coerceAtLeast(0)).toString()
    }

    /** Номер текущего круга и следующее упражнение (у которого меньше всего подходов). */
    fun supersetProgress(supersetId: String): Pair<Int, ExerciseInstance?> {
        val members = supersetMembers(supersetId)
        val counts = members.map { instanceEntries(it).size }
        val min = counts.minOrNull() ?: 0
        val next = members.firstOrNull { instanceEntries(it).size == min }
        return (min + 1) to next
    }

    fun startSuperset(instanceId: String) {
        sheet = Sheet.ExerciseChooser(supersetWith = instanceId)
    }

    private fun addToSuperset(targetId: String, exerciseId: String) {
        val s = session ?: return
        val target = s.exerciseInstances.firstOrNull { it.instanceId == targetId } ?: return
        val groupId = target.supersetId ?: uid("superset").also { id ->
            target.supersetId = id
            instanceEntries(target).forEach { it.supersetId = id }
        }
        if (supersetMembers(groupId).any { it.exerciseId == exerciseId }) {
            showToast("Это упражнение уже в суперсете")
            return
        }
        // Повторно добавленное упражнение отделяется от прежнего экземпляра, чтобы не смешивать подходы.
        val instance = ExerciseInstance(uid("exercise"), exerciseId, supersetId = groupId)
        // Новое упражнение встаёт сразу над группой: группа остаётся одной карточкой.
        val firstIndex = s.exerciseInstances.indexOfFirst { it.supersetId == groupId }.coerceAtLeast(0)
        s.exerciseInstances.add(firstIndex, instance)
        supersetMembers(groupId).forEach { it.completed = false }
        sheet = null
        changed()
        val letters = supersetMembers(groupId).joinToString(" + ") { calc.exercise(it.exerciseId).name }
        showToast("Суперсет: $letters")
    }

    /** Убрать упражнение из суперсета; если в группе осталось одно, суперсет распадается. */
    fun leaveSuperset(instanceId: String) {
        val s = session ?: return
        val instance = s.exerciseInstances.firstOrNull { it.instanceId == instanceId } ?: return
        val groupId = instance.supersetId ?: return
        instance.supersetId = null
        instanceEntries(instance).forEach { it.supersetId = null }
        if (instanceEntries(instance).isEmpty()) s.exerciseInstances.remove(instance)
        val rest = supersetMembers(groupId)
        if (rest.size == 1) {
            rest[0].supersetId = null
            instanceEntries(rest[0]).forEach { it.supersetId = null }
        }
        changed()
    }

    fun finishSuperset(supersetId: String) {
        val s = session ?: return
        val members = supersetMembers(supersetId)
        val names = members.joinToString(" и ") { "«${calc.exercise(it.exerciseId).name}»" }
        ask("Завершить суперсет?", "$names будут завершены.", "Завершить") {
            members.forEach { m -> if (instanceEntries(m).isEmpty()) s.exerciseInstances.remove(m) else m.completed = true }
            changed()
        }
    }

    fun reopenSuperset(supersetId: String) {
        supersetMembers(supersetId).forEach { it.completed = false }
        changed()
    }

    fun selectSessionExercise(exerciseId: String, supersetWith: String? = null) {
        val s = session ?: return
        if (supersetWith != null) { addToSuperset(supersetWith, exerciseId); return }
        val unfinished = s.exerciseInstances.filter { !it.completed && it.exerciseId != exerciseId }
        val activate = {
            unfinished.forEach { it.completed = true }
            val instance = addSessionExercise(exerciseId)
            changed()
            openSetEntry(exerciseId, instance.instanceId)
        }
        if (unfinished.isEmpty()) { activate(); return }
        sheet = null
        val names = unfinished.joinToString(", ") { calc.exercise(it.exerciseId).name }
        ask("Завершить текущее упражнение?", "«$names» будет завершено, а выбранное упражнение откроется сверху.", "Завершить и добавить") { activate() }
    }

    fun openSetEntry(exerciseId: String, instanceId: String) {
        pendingSetHrSamples.clear()
        sheet = Sheet.SetEntry(exerciseId, instanceId)
    }

    data class SetDefaults(val weight: Double, val reps: Int, val weighted: Boolean, val setIndex: Int)

    fun setDefaults(exerciseId: String, instanceId: String): SetDefaults {
        val exercise = calc.exercise(exerciseId)
        val mode = calc.loadMode(exercise)
        val instance = session?.exerciseInstances?.firstOrNull { it.instanceId == instanceId }
        val existing = instance?.let { instanceEntries(it) } ?: session?.entries.orEmpty().filter { !it.isCardio && it.exerciseId == exerciseId }
        val previous = existing.lastOrNull()
        val supportsBodyweight = exercise.bodyweight?.supported == true
        val weighted = previous?.weighted ?: if (supportsBodyweight) exercise.bodyweight?.defaultWeighted == true else true
        val base = exercise.baseWeight?.takeIf { it > 0 } ?: when (mode) { "barbell" -> 20.0; "smith" -> 9.0; else -> 0.0 }
        return if (previous != null) SetDefaults(if (previous.weight > 0) previous.weight else base, max(1, previous.reps), weighted, existing.size)
        else SetDefaults(base, 1, weighted, existing.size)
    }

    /** Варианты веса для колеса выбора — те же ряды, что в app.js (гантельные ряды, блины 2,5 кг и т. д.). */
    fun weightOptions(exercise: Exercise): List<Double> {
        val mode = calc.loadMode(exercise)
        val name = exercise.name.lowercase(Calc.RU_LOCALE)
        val base = exercise.baseWeight?.takeIf { it > 0 } ?: when (mode) { "barbell" -> 20.0; "smith" -> 9.0; else -> 0.0 }
        fun range(start: Double, end: Double, step: Double) =
            (0..((end - start) / step).toInt()).map { Math.round((start + it * step) * 100) / 100.0 }
        return when {
            mode == "paired-dumbbell" || mode == "single-dumbbell" ->
                (range(1.0, 10.0, 1.0) + range(12.0, 60.0, 2.0) + range(12.5, 60.0, 2.5) + range(65.0, 100.0, 5.0)).distinct().sorted()
            "гир" in name -> listOf(4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 24.0, 28.0, 32.0, 36.0, 40.0, 44.0, 48.0)
            mode == "barbell" -> range(max(20.0, base), 300.0, 2.5)
            mode == "smith" -> range(max(9.0, base), 250.0, 2.5)
            mode == "assisted" -> range(0.0, 150.0, 2.5)
            exercise.bodyweight?.supported == true -> range(1.25, 100.0, 1.25)
            listOf("рычаж", "жим ногами", "гакк", "саней", "санки").any { it in name } -> range(0.0, 300.0, 2.5)
            listOf("тренаж", "блок", "кроссовер").any { it in name } -> range(2.5, 250.0, 2.5)
            else -> range(0.0, 250.0, 2.5)
        }
    }

    fun addSet(exerciseId: String, instanceId: String, weight: Double, reps: Int, weighted: Boolean) {
        val s = session ?: return
        val exercise = calc.exercise(exerciseId)
        val supportsBodyweight = exercise.bodyweight?.supported == true
        val isWeighted = if (supportsBodyweight) weighted else true
        val avgHr = if (pendingSetHrSamples.isEmpty()) 0 else pendingSetHrSamples.average().roundToInt()
        val instance = s.exerciseInstances.firstOrNull { it.instanceId == instanceId }
        s.entries += Entry(
            type = "strength",
            exerciseId = exerciseId,
            instanceId = instanceId,
            supersetId = instance?.supersetId,
            set = setDefaults(exerciseId, instanceId).setIndex,
            weight = if (isWeighted) weight else 0.0,
            reps = max(1, reps),
            weighted = isWeighted,
            bodyweightMode = supportsBodyweight,
            bodyWeight = state.profile.weight ?: 0.0,
            avgHr = avgHr,
            hrSamples = pendingSetHrSamples.toMutableList(),
        )
        pendingSetHrSamples.clear()
        sheet = null
        // Суперсет: после подхода сразу следующее упражнение круга, отдых — после последнего.
        val groupId = instance?.supersetId
        val members = groupId?.let { supersetMembers(it) }.orEmpty()
        val next = members.getOrNull(members.indexOf(instance) + 1)
        if (groupId != null && next != null && s.editingWorkoutId == null) {
            changed()
            openSetEntry(next.exerciseId, next.instanceId)
            return
        }
        // Отдых между подходами начинается сам — не нужно нажимать «Старт».
        if (s.editingWorkoutId == null) startRest()
        changed()
    }

    /** Удаление ошибочного подхода; номера оставшихся подходов упражнения пересчитываются. */
    fun deleteSet(entry: Entry) {
        val s = session ?: return
        if (!s.entries.removeAll { it === entry }) return
        s.exerciseInstances.firstOrNull { it.instanceId == entry.instanceId }?.let { instance ->
            instanceEntries(instance).forEachIndexed { index, e -> e.set = index }
        }
        changed()
        showToast("Подход удалён")
    }

    fun finishExercise(instanceId: String) {
        val s = session ?: return
        val instance = s.exerciseInstances.firstOrNull { it.instanceId == instanceId } ?: return
        ask("Завершить упражнение?", "Точно закончить «${calc.exercise(instance.exerciseId).name}»?", "Завершить") {
            if (instanceEntries(instance).isEmpty()) s.exerciseInstances.remove(instance) else instance.completed = true
            changed()
        }
    }

    fun reopenExercise(instanceId: String) {
        session?.exerciseInstances?.firstOrNull { it.instanceId == instanceId }?.let {
            it.completed = false
            changed()
        }
    }

    // ---------- кардио ----------

    fun startCardio(activityId: String) {
        val s = session ?: return
        sheet = null
        val available = calc.visibleCardioOptions()
        if (available.isEmpty()) { showToast("Нет доступных вариантов кардио"); return }
        if (s.editingWorkoutId != null) { sheet = Sheet.CardioManual(activityId); return }
        s.entries.firstOrNull { it.isRunningCardio }?.let {
            showToast("Сначала завершите «${it.cardioName}»")
            return
        }
        val activity = calc.cardioOption(available.firstOrNull { it.id == activityId }?.id ?: available.first().id)
        s.entries.add(0, Entry(type = "cardio", cardioEntryId = uid("cardio"), cardioId = activity.id, cardioName = activity.name, startedAt = System.currentTimeMillis(), completed = false, hrSamples = mutableListOf()))
        startTicker()
        changed()
    }

    fun openCardioFinish(cardioEntryId: String) {
        sheet = Sheet.CardioFinish(cardioEntryId, System.currentTimeMillis())
    }

    fun finishCardio(cardioEntryId: String, requestedAt: Long, distance: Double, resistance: Double, avgHr: Int) {
        val entry = session?.entries?.firstOrNull { it.cardioEntryId == cardioEntryId && it.completed == false } ?: return
        val activity = calc.cardioOption(entry.cardioId)
        val durationSeconds = max(1L, (requestedAt - (entry.startedAt ?: requestedAt)) / 1000)
        val speed = if (distance > 0) distance / (durationSeconds / 3600.0) else 0.0
        entry.endedAt = requestedAt
        entry.durationSeconds = durationSeconds
        entry.minutes = Math.round(durationSeconds / 60.0 * 10) / 10.0
        entry.distance = distance
        entry.resistance = resistance
        entry.avgHr = avgHr
        entry.speed = Math.round(speed * 10) / 10.0
        entry.calories = calc.cardioCalories(activity, durationSeconds / 60.0, speed, 0.0, resistance, avgHr)
        entry.completed = true
        entry.hrSamples = null
        sheet = null
        changed()
    }

    fun addManualCardio(activityId: String, minutes: Double, distance: Double, resistance: Double, avgHr: Int) {
        val s = session ?: return
        if (minutes <= 0) return
        val activity = calc.cardioOption(activityId)
        val speed = if (distance > 0) distance / (minutes / 60.0) else 0.0
        s.entries.add(0, Entry(
            type = "cardio", cardioEntryId = uid("cardio"), cardioId = activity.id, cardioName = activity.name,
            completed = true, durationSeconds = (minutes * 60).roundToInt().toLong(), minutes = minutes,
            distance = distance, resistance = resistance, avgHr = avgHr, speed = Math.round(speed * 10) / 10.0,
            calories = calc.cardioCalories(activity, minutes, speed, 0.0, resistance, avgHr),
        ))
        sheet = null
        changed()
    }

    // ---------- профиль ----------

    fun saveProfile(name: String, gender: String, age: Int, weight: Double, height: Double, photo: String, fromOnboarding: Boolean) {
        val p = state.profile
        val previousWeight = p.weight ?: 0.0
        p.name = name.trim().ifEmpty { if (fromOnboarding) "Гость" else "" }
        p.gender = gender
        p.age = age
        p.weight = weight
        p.height = height
        p.photo = photo
        if (weight > 0 && weight != previousWeight) p.weights += WeightPoint(today(), Calc.nowTime(), weight)
        state.profileOnboardingSkipped = false
        changed()
        if (!fromOnboarding) {
            sheet = Sheet.Profile
            showToast("Профиль сохранён")
        }
    }

    fun addWeighing(weight: Double) {
        if (weight <= 0) return
        state.profile.weight = weight
        state.profile.weights += WeightPoint(today(), Calc.nowTime(), weight)
        changed()
        sheet = Sheet.Profile
        showToast("Вес сохранён")
    }

    // ---------- библиотека упражнений ----------

    fun saveExercise(id: String?, name: String, category: String) {
        val normalized = Catalog.normalizeCategory(category)
        val muscle = if (normalized == Catalog.UNCATEGORIZED) "" else normalized
        val editing = id?.let { state.exercises.firstOrNull { e -> e.id == it } }
        if (editing != null) {
            editing.category = normalized
            editing.muscle = muscle
        } else {
            if (name.isBlank()) return
            state.exercises.add(0, Exercise(id = uid("ex"), name = name.trim(), category = normalized, muscle = muscle))
        }
        sheet = null
        changed()
    }

    fun deleteExercise(id: String) {
        val exercise = calc.exercise(id)
        ask("Удалить упражнение?", "«${exercise.name}» пропадёт из библиотеки и сплитов. История тренировок сохранится.", "Удалить") {
            state.exercises.firstOrNull { it.id == id }?.let {
                it.archived = true
                it.universal = false
                it.splitIds.clear()
            }
            changed()
            showToast("Упражнение удалено")
        }
    }

    fun deleteCardio(id: String) {
        val activity = calc.cardioOption(id)
        ask("Удалить кардио?", "«${activity.name}» исчезнет из библиотеки и выбора кардио. Сохранённые тренировки останутся.", "Удалить") {
            if (id !in state.cardioHiddenIds) state.cardioHiddenIds += id
            state.cardioNames.remove(id)
            changed()
            showToast("Кардио удалено")
        }
    }

    // ---------- сплиты ----------

    fun saveSplit(id: String?, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val editing = id?.let { state.splits.firstOrNull { s -> s.id == it } }
        if (editing != null) {
            editing.name = trimmed
            sheet = null
            changed()
            return
        }
        val created = Split(uid("split"), trimmed, calc.nextSplitColor())
        state.splits += created
        if (created.id !in expandedSplits) expandedSplits += created.id
        changed()
        sheet = Sheet.SplitPicker(created.id)
    }

    fun deleteSplit(id: String) {
        val split = calc.split(id)
        ask("Удалить сплит?", "Сплит «${split.name}» скроется из списка. Тренировки в журнале сохранят его название и цвет.", "Удалить") {
            state.splits.firstOrNull { it.id == id }?.archived = true
            state.exercises.forEach { it.splitIds.remove(id) }
            expandedSplits.remove(id)
            changed()
            showToast("Сплит удалён")
        }
    }

    fun toggleSplit(id: String) {
        if (id in expandedSplits) expandedSplits.remove(id) else expandedSplits += id
    }

    fun addExercisesToSplit(splitId: String, ids: Set<String>) {
        if (ids.isEmpty()) return
        val isOther = splitId == "other"
        ids.forEach { exerciseId ->
            val exercise = state.exercises.firstOrNull { it.id == exerciseId } ?: return@forEach
            if (isOther) {
                exercise.universal = true
                exercise.splitIds.clear()
            } else if (splitId !in exercise.splitIds) exercise.splitIds += splitId
        }
        if (splitId !in expandedSplits) expandedSplits += splitId
        sheet = null
        changed()
        val label = if (isOther) state.otherName else calc.split(splitId).name
        val count = ids.size
        showToast("$count ${plural(count, "упражнение добавлено", "упражнения добавлены", "упражнений добавлено")} в «$label»")
    }

    fun removeExerciseFromSplit(exerciseId: String, splitId: String) {
        val exercise = state.exercises.firstOrNull { it.id == exerciseId } ?: return
        ask("Убрать упражнение?", "«${exercise.name}» перестанет отображаться в этом сплите.", "Убрать") {
            exercise.splitIds.remove(splitId)
            changed()
            showToast("Упражнение убрано из сплита")
        }
    }
}
