package com.example.pulseworkout.logic

import com.example.pulseworkout.data.AppState
import com.example.pulseworkout.data.Catalog
import com.example.pulseworkout.data.CardioOption
import com.example.pulseworkout.data.Entry
import com.example.pulseworkout.data.Exercise
import com.example.pulseworkout.data.Profile
import com.example.pulseworkout.data.Split
import com.example.pulseworkout.data.Workout
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Порт вычислительных функций app.js: тоннаж, калории, форматирование, периоды. */
class Calc(private val state: () -> AppState) {
    private val s get() = state()

    val missingSplit = Split("", "Без сплита", "lime")
    val missingExercise = Exercise("", "Удалённое упражнение")

    fun split(id: String?) = s.splits.firstOrNull { it.id == id } ?: missingSplit
    fun exercise(id: String?) = s.exercises.firstOrNull { it.id == id } ?: missingExercise

    fun exerciseCategory(exercise: Exercise?): String {
        val category = Catalog.normalizeCategory(exercise?.category?.ifBlank { exercise.muscle } ?: exercise?.muscle)
        return if (category in Catalog.categories) category else Catalog.UNCATEGORIZED
    }

    fun categoryOf(exercise: Exercise) = if (exercise.cardio) "Кардио" else exerciseCategory(exercise)

    fun sortCategories(categories: Collection<String>): List<String> {
        val order = Catalog.categories.withIndex().associate { it.value to it.index }
        return categories.distinct().sortedWith { a, b ->
            when {
                a == Catalog.UNCATEGORIZED -> -1
                b == Catalog.UNCATEGORIZED -> 1
                else -> ((order[a] ?: 999) - (order[b] ?: 999)).takeIf { it != 0 } ?: RU.compare(a, b)
            }
        }
    }

    fun visibleCardioOptions(): List<CardioOption> {
        val hidden = s.cardioHiddenIds.toSet()
        return Catalog.cardioOptions.filter { it.id !in hidden }.map { it.copy(name = s.cardioNames[it.id] ?: it.name) }
    }

    fun cardioOption(id: String?): CardioOption {
        val base = Catalog.cardioOptions.firstOrNull { it.id == id } ?: Catalog.cardioOptions.first()
        return base.copy(name = s.cardioNames[base.id] ?: base.name)
    }

    /** Упражнения библиотеки без удалённых пользователем. */
    fun activeExercises(): List<Exercise> = s.exercises.filter { !it.archived }

    fun orderedSplits(): List<Split> {
        val order = listOf("legs", "push", "pull", "arms")
        return s.splits.filter { !it.archived }.sortedWith { a, b ->
            val ai = order.indexOf(a.id)
            val bi = order.indexOf(b.id)
            if (ai != -1 || bi != -1) (if (ai == -1) 99 else ai) - (if (bi == -1) 99 else bi)
            else RU.compare(a.name, b.name)
        }
    }

    fun nextSplitColor(): String {
        val active = s.splits.filter { !it.archived }
        val used = active.map { it.color }.toSet()
        return Catalog.colors.firstOrNull { it !in used } ?: Catalog.colors[active.size % Catalog.colors.size]
    }

    // ---------- нагрузка ----------

    fun loadMode(exercise: Exercise?): String {
        exercise?.loadMode?.let { return it }
        val name = exercise?.name.orEmpty().lowercase(RU_LOCALE).replace('ё', 'е')
        if (exercise?.id == "assisted-pullup" || "гравитрон" in name) return "assisted"
        if ("гантел" in name) {
            val single = listOf("двумя руками", "двумя рукам", "из-за головы", "пуловер", "гоблет", "кубков")
            return if (single.any { it in name }) "single-dumbbell" else "paired-dumbbell"
        }
        if ("штанг" in name) return "barbell"
        return "standard"
    }

    private fun bodyWeightFor(entry: Entry) = if (entry.bodyWeight > 0) entry.bodyWeight else (s.profile.weight ?: 0.0)

    fun entryTonnage(entry: Entry): Double {
        if (entry.isCardio) return 0.0
        val weight = max(0.0, entry.weight)
        val reps = max(0, entry.reps)
        val exercise = exercise(entry.exerciseId)
        val mode = loadMode(exercise)
        if (mode == "assisted") return max(0.0, bodyWeightFor(entry) - weight) * reps
        val bw = exercise.bodyweight
        if (entry.bodyweightMode && bw?.supported == true) {
            val effective = bodyWeightFor(entry) * bw.fraction.coerceIn(0.0, 1.0)
            val external = if (!entry.weighted) 0.0 else if (mode == "paired-dumbbell") weight * 2 else weight
            return (effective + external) * reps
        }
        if (mode == "paired-dumbbell") return weight * reps * 2
        return weight * reps
    }

    /**
     * Нагрузка одного повторения с учётом режима: для гравитрона — вес тела минус помощь,
     * для упражнений с весом тела — доля веса тела плюс утяжеление. Используется для рекордов
     * (в app.js сравнивался вес на снаряде, что неверно для гравитрона и отжиманий).
     */
    fun effectiveLoad(entry: Entry): Double = if (entry.reps <= 0) 0.0 else entryTonnage(entry) / entry.reps

    /** Подходы-рекорды тренировки: лучшая нагрузка в упражнении выше, чем во всех более ранних тренировках. */
    fun recordEntries(workout: Workout): Set<Entry> {
        val older = s.workouts.filter { newestFirst.compare(it, workout) > 0 }
        return workout.entries.filter { !it.isCardio }.groupBy { it.exerciseId }.flatMap { (exerciseId, entries) ->
            val best = entries.maxOf { effectiveLoad(it) }
            val previousBest = older.flatMap { w -> w.entries.filter { !it.isCardio && it.exerciseId == exerciseId } }
                .maxOfOrNull { effectiveLoad(it) } ?: 0.0
            if (previousBest > 0 && best > previousBest + 1e-6) entries.filter { abs(effectiveLoad(it) - best) < 1e-6 } else emptyList()
        }.toSet()
    }

    fun formatEntryWeight(entry: Entry): String {
        val weight = max(0.0, entry.weight)
        val mode = loadMode(exercise(entry.exerciseId))
        if (mode == "assisted") return if (weight > 0) "${num(weight)} кг помощи" else "без помощи"
        if (entry.bodyweightMode && !entry.weighted) return "без утяжеления"
        if (weight <= 0) return "без утяжеления"
        if (mode == "paired-dumbbell") return "${num(weight)} кг / рука"
        return "${num(weight)} кг"
    }

    data class Summary(val tonnage: Long, val reps: Int, val sets: Int)

    fun summary(entries: List<Entry>): Summary {
        var tonnage = 0.0
        var reps = 0
        var sets = 0
        entries.filter { !it.isCardio }.forEach {
            tonnage += entryTonnage(it)
            reps += it.reps
            sets += 1
        }
        return Summary(tonnage.roundToLong(), reps, sets)
    }

    // ---------- калории ----------

    private fun restingCaloriesPerMinute(): Double {
        val p = s.profile
        val weight = p.weight?.takeIf { it > 0 } ?: 70.0
        val height = p.height?.takeIf { it > 0 } ?: 175.0
        val age = p.age?.takeIf { it > 0 } ?: 30
        val sexOffset = when (p.gender) { "female" -> -161; "male" -> 5; else -> -78 }
        val bmr = max(1000.0, 10 * weight + 6.25 * height - 5 * age + sexOffset)
        return bmr / 1440
    }

    private fun caloriesFromMet(met: Double, minutes: Double) =
        max(0.0, (restingCaloriesPerMinute() * max(0.0, met) * max(0.0, minutes)).roundToInt().toDouble())

    /** Формула Keytel (2005); null, если для неё не хватает данных. */
    private fun caloriesFromHeartRate(avgHr: Int, minutes: Double): Double? {
        val p = s.profile
        val weight = p.weight ?: 0.0
        val age = p.age ?: 0
        val duration = max(0.0, minutes)
        if (duration <= 0 || weight <= 0 || age <= 0 || avgHr < 40 || avgHr > 240 || p.gender !in listOf("male", "female")) return null
        val kjPerMinute = if (p.gender == "male") -55.0969 + 0.6309 * avgHr + 0.1988 * weight + 0.2017 * age
        else -20.4022 + 0.4472 * avgHr - 0.1263 * weight + 0.074 * age
        return max(0.0, (kjPerMinute / 4.184 * duration).roundToInt().toDouble())
    }

    fun cardioCalories(activity: CardioOption, minutes: Double, speed: Double = 0.0, incline: Double = 0.0, resistance: Double = 0.0, avgHr: Int = 0): Double {
        caloriesFromHeartRate(avgHr, minutes)?.let { return it }
        val factor = if (activity.id == "treadmill") max(0.0, speed - 6) * 0.14 + incline * 0.025 else max(0.0, resistance) * 0.02
        return caloriesFromMet(activity.met * (1 + factor), minutes)
    }

    fun estimateCalories(entry: Entry): Double {
        if (entry.isCardio) {
            return cardioCalories(cardioOption(entry.cardioId), entry.minutes, entry.speed, entry.incline, entry.resistance, entry.avgHr)
        }
        val exercise = exercise(entry.exerciseId)
        val setMinutes = (1.4 + entry.reps * 0.04).coerceIn(1.2, 3.0)
        val bw = exercise.bodyweight
        val met = if (entry.bodyweightMode && bw?.supported == true) bw.met ?: exercise.met ?: 3.5
        else exercise.calorieModel?.met ?: exercise.met ?: 3.5
        return caloriesFromHeartRate(entry.avgHr, setMinutes) ?: caloriesFromMet(met, setMinutes)
    }

    fun entryCalories(entry: Entry) = if (entry.calories > 0) entry.calories else estimateCalories(entry)

    // ---------- тренировки ----------

    data class Metrics(val calories: Long, val tonnage: Long, val sets: Int, val durationSeconds: Long)

    fun metrics(workout: Workout): Metrics {
        val sum = summary(workout.entries)
        return Metrics(workout.entries.sumOf { entryCalories(it) }.roundToLong(), sum.tonnage, sum.sets, workout.durationSeconds)
    }

    /**
     * Изменение показателя относительно предыдущей тренировки того же сплита: (рост?, процент).
     * Предыдущая — строго более ранняя (по дате и времени создания); в app.js в один день
     * более поздняя тренировка могла считаться «предыдущей».
     */
    fun trend(workout: Workout, value: (Metrics) -> Long): Pair<Boolean, Int>? {
        val previous = s.workouts.filter { it.splitId == workout.splitId && newestFirst.compare(it, workout) > 0 }
            .sortedWith(newestFirst).firstOrNull() ?: return null
        val prev = value(metrics(previous))
        val current = value(metrics(workout))
        if (prev == 0L || current == prev) return null
        val percent = min(9999, (abs(current - prev).toDouble() / prev * 100).roundToInt())
        return (current > prev) to percent
    }

    data class PeriodStats(val workouts: Int, val sets: Int, val tonnage: Long, val calories: Long, val durationSeconds: Long, val cardioMinutes: Double)

    /** Сводка за период (порт getStats из app.js). */
    fun periodStats(workouts: List<Workout>): PeriodStats {
        var sets = 0
        var tonnage = 0.0
        var calories = 0.0
        var cardio = 0.0
        workouts.forEach { w ->
            w.entries.forEach { e ->
                calories += entryCalories(e)
                if (e.isCardio) cardio += e.minutes else { sets++; tonnage += entryTonnage(e) }
            }
        }
        return PeriodStats(workouts.size, sets, tonnage.roundToLong(), calories.roundToLong(), workouts.sumOf { it.durationSeconds }, cardio)
    }

    val newestFirst = Comparator<Workout> { a, b ->
        val byDate = b.date.compareTo(a.date)
        if (byDate != 0) byDate else workoutTimestamp(b).compareTo(workoutTimestamp(a))
    }

    private fun workoutTimestamp(w: Workout) = Regex("^[^-]+-(\\d{10,})-").find(w.id)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

    fun cardioElapsedSeconds(entry: Entry, now: Long = System.currentTimeMillis()): Long {
        val start = entry.startedAt ?: return max(0L, entry.durationSeconds)
        return max(0L, ((entry.endedAt ?: now) - start) / 1000)
    }

    fun cardioAverageHr(entry: Entry): Int {
        val samples = entry.hrSamples.orEmpty().filter { it > 0 }
        return if (samples.isNotEmpty()) samples.average().roundToInt() else entry.avgHr
    }

    fun exerciseAverageHr(entries: List<Entry>): Int {
        val samples = entries.flatMap { e -> e.hrSamples?.takeIf { it.isNotEmpty() } ?: (if (e.avgHr > 0) listOf(e.avgHr) else emptyList()) }
        return if (samples.isEmpty()) 0 else samples.average().roundToInt()
    }

    // ---------- профиль ----------

    fun bmi(weight: Double?, height: Double?): Double? {
        val w = weight ?: return null
        val h = (height ?: return null) / 100
        if (w <= 0 || h <= 0) return null
        return w / (h * h)
    }

    fun bmiCategory(profile: Profile): String {
        val v = bmi(profile.weight, profile.height) ?: return "—"
        return when {
            v < 18.5 -> "Недостаток веса"
            v < 25 -> "Норма"
            v < 30 -> "Избыточный вес"
            v < 35 -> "Ожирение I"
            v < 40 -> "Ожирение II"
            else -> "Ожирение III"
        }
    }

    private fun sortedWeights(profile: Profile) = profile.weights.filter { it.weight > 0 }.sortedBy { "${it.date} ${it.time}" }

    /** true — вес снизился, false — вырос, null — без изменений или мало данных. */
    fun weightTrendDown(profile: Profile): Boolean? {
        val points = sortedWeights(profile)
        if (points.size < 2) return null
        val current = points.last().weight
        val previous = points[points.size - 2].weight
        return if (current == previous) null else current < previous
    }

    fun bmiTrendDown(profile: Profile): Boolean? {
        if (profile.height == null) return null
        val points = sortedWeights(profile)
        if (points.size < 2) return null
        val current = bmi(points.last().weight, profile.height) ?: return null
        val previous = bmi(points[points.size - 2].weight, profile.height) ?: return null
        return if (current == previous) null else current < previous
    }

    fun isProfileComplete(p: Profile = s.profile) =
        p.gender in listOf("male", "female") && (p.weight ?: 0.0) > 0 && (p.age ?: 0) >= 10 && (p.height ?: 0.0) >= 80

    companion object {
        val RU_LOCALE: Locale = Locale.forLanguageTag("ru-RU")
        val RU: java.text.Collator = java.text.Collator.getInstance(RU_LOCALE)

        val monthNames = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
        val fullMonthNames = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
        val weekdays = listOf("Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье")

        fun today(): String = LocalDate.now().toString()
        fun nowTime(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

        fun periodStart(end: String, period: String): String =
            LocalDate.parse(end).minusDays(when (period) { "week" -> 6L; "year" -> 364L; else -> 29L }).toString()

        fun periodName(period: String) = when (period) { "week" -> "неделю"; "year" -> "год"; else -> "месяц" }

        fun shortDate(value: String): String = LocalDate.parse(value).let { "${it.dayOfMonth} ${monthNames[it.monthValue - 1]}" }
        fun journalDateLabel(value: String): String = LocalDate.parse(value).let {
            "${it.dayOfMonth} ${fullMonthNames[it.monthValue - 1]}"
        }
        fun weekday(value: String): String = weekdays[LocalDate.parse(value).dayOfWeek.value - 1]

        fun plural(number: Int, one: String, few: String, many: String): String {
            val n = abs(number) % 100
            val last = n % 10
            return when {
                n in 11..19 -> many
                last in 2..4 -> few
                last == 1 -> one
                else -> many
            }
        }

        fun clock(seconds: Long): String {
            val total = max(0L, seconds)
            return "%02d:%02d".format(total / 60, total % 60)
        }

        fun duration(seconds: Long): String {
            val totalMinutes = max(0L, (seconds / 60.0).roundToLong())
            if (totalMinutes == 0L) return "0 мин"
            if (totalMinutes < 60) return "$totalMinutes мин"
            val h = totalMinutes / 60
            val m = totalMinutes % 60
            return if (m > 0) "$h ч $m мин" else "$h ч"
        }

        private val grouping = DecimalFormat("#,##0", DecimalFormatSymbols(RU_LOCALE).apply { groupingSeparator = ' ' })
        private val decimal = DecimalFormat("#,##0.##", DecimalFormatSymbols(RU_LOCALE).apply { groupingSeparator = ' ' })

        fun tonnage(value: Long) = "${grouping.format(value)} кг"
        fun grouped(value: Long): String = grouping.format(value)
        fun num(value: Double): String = decimal.format(value)

        fun ageLabel(age: Int): String {
            val tail = age % 10
            val teens = age % 100 in 11..14
            return "$age " + when {
                !teens && tail == 1 -> "год"
                !teens && tail in 2..4 -> "года"
                else -> "лет"
            }
        }

        /** Нормализация для поиска: регистр и ё/е. */
        fun searchKey(value: String) = value.lowercase(RU_LOCALE).replace('ё', 'е').trim()

        fun uid(prefix: String): String =
            "$prefix-${System.currentTimeMillis()}-${(0 until 5).map { "0123456789abcdef".random() }.joinToString("")}"
    }
}
