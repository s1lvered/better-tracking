package com.example.pulseworkout.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pulseworkout.data.Entry
import com.example.pulseworkout.data.Workout
import com.example.pulseworkout.logic.Calc
import com.example.pulseworkout.ui.AppViewModel
import com.example.pulseworkout.ui.BottomSpacer
import com.example.pulseworkout.ui.Sheet
import com.example.pulseworkout.ui.components.Btn
import com.example.pulseworkout.ui.components.BtSheet
import com.example.pulseworkout.ui.components.CardShape
import com.example.pulseworkout.ui.components.EmptyState
import com.example.pulseworkout.ui.components.GlowDot
import com.example.pulseworkout.ui.components.KpiTile
import com.example.pulseworkout.ui.components.MetaRow
import com.example.pulseworkout.ui.components.RoundIconButton
import com.example.pulseworkout.ui.components.SupersetBadge
import com.example.pulseworkout.ui.components.SupersetLetter
import androidx.compose.foundation.layout.fillMaxHeight
import com.example.pulseworkout.ui.theme.Bt
import com.example.pulseworkout.ui.theme.BtType
import com.example.pulseworkout.ui.theme.Ds
import com.example.pulseworkout.ui.theme.DsIcons
import com.example.pulseworkout.ui.theme.DsType
import java.time.LocalDate

@Composable
fun JournalScreen(vm: AppViewModel) {
    // «Сегодня» вычисляется при каждой отрисовке: после полуночи журнал не теряет новые тренировки.
    val today = Calc.today()
    val start = Calc.periodStart(today, vm.journalPeriod)
    val workouts = vm.state.workouts.filter { it.date in start..today }.sortedWith(vm.calc.newestFirst)
    val current = vm.session?.takeIf { it.editingWorkoutId == null }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { JournalTitle(vm, today) }
        if (workouts.isNotEmpty()) item {
            Spacer(Modifier.height(16.dp))
            PeriodStats(vm, workouts)
        }
        if (current != null) item {
            Spacer(Modifier.height(24.dp))
            ActiveTrainingCard(vm)
        }
        itemsIndexed(workouts, key = { _, w -> w.id }) { index, workout ->
            if (index == 0) Spacer(Modifier.height(if (current != null) 12.dp else 24.dp))
            else HorizontalDivider(thickness = 1.dp, color = Ds.whiteOpacity)
            TrainingRow(vm, workout)
        }
        if (workouts.isEmpty() && current == null) item {
            Box(Modifier.fillMaxWidth().padding(top = 120.dp)) {
                // Макеты «Journal / 1 First Run» и «5 Empty Period»
                val firstRun = vm.state.workouts.isEmpty()
                EmptyState(
                    title = if (firstRun) "Первая тренировка ждёт" else "За этот период записей нет",
                    text = if (firstRun) "Создайте сплит и начните —\nвсё запишется сюда" else "Выберите другой период\nили начните новую тренировку",
                    action = { Btn("Начать тренировку", vm.feedback, Modifier.fillMaxWidth(0.75f), icon = DsIcons.Dumbbell) { vm.requestStartSession() } },
                )
            }
        }
        item { BottomSpacer() }
    }
}

/** PeriodTitle: дата и «Журнал за …» с выпадающим выбором периода. */
@Composable
private fun JournalTitle(vm: AppViewModel, today: String) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text("${Calc.journalDateLabel(today)} · ${Calc.weekday(today)}", style = DsType.Body2R, color = Ds.whiteTertiary)
        Box {
            Row(Modifier.clip(RoundedCornerShape(8.dp)).clickable { vm.feedback.haptic(); open = true }, verticalAlignment = Alignment.CenterVertically) {
                Text("Журнал ", style = DsType.Headline1B)
                Text("за ${Calc.periodName(vm.journalPeriod)}", style = DsType.Headline1B, color = Ds.lime)
                Icon(DsIcons.ChevronDown, null, tint = Ds.lime, modifier = Modifier.padding(start = 4.dp).size(14.dp))
            }
            DropdownMenu(open, onDismissRequest = { open = false }, containerColor = Ds.sheet, modifier = Modifier.border(1.dp, Ds.whiteOpacity, RoundedCornerShape(4.dp))) {
                listOf("week", "month", "year").forEach { period ->
                    DropdownMenuItem(
                        text = { Text("за ${Calc.periodName(period)}", style = DsType.Body3R, color = if (period == vm.journalPeriod) Ds.lime else Ds.white) },
                        onClick = { vm.journalPeriod = period; open = false },
                    )
                }
            }
        }
    }
}

/** Сводка периода — четыре StatsTab. */
@Composable
private fun PeriodStats(vm: AppViewModel, workouts: List<Workout>) {
    val stats = vm.calc.periodStats(workouts)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiTile(DsIcons.Dumbbell, "Тренировки", "${stats.workouts}", Modifier.weight(1f))
            KpiTile(DsIcons.Flame, "Калории", Calc.grouped(stats.calories), Modifier.weight(1f), unit = "ккал")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiTile(DsIcons.Weight, "Тоннаж", Calc.grouped(stats.tonnage), Modifier.weight(1f), unit = "кг")
            KpiTile(DsIcons.Clock, "Время", Calc.duration(stats.durationSeconds), Modifier.weight(1f))
        }
    }
}

/** ActiveTrainingCard: Black/Tertiary с обводкой Lime/Accent; нажатие возвращает к тренировке. */
@Composable
private fun ActiveTrainingCard(vm: AppViewModel) {
    val s = vm.session ?: return
    val split = vm.calc.split(s.splitId)
    val summary = vm.calc.summary(s.entries)
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(Ds.blackTertiary).border(1.dp, Ds.lime, CardShape)
            .clickable { vm.tab = "log" }.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Активная тренировка", style = DsType.Label2R, color = Ds.lime)
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlowDot(Bt.color(split.color))
                Spacer(Modifier.width(6.dp))
                Text(split.name, style = DsType.Title2B, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            MetaRow(
                listOf(
                    DsIcons.Exercise to "${summary.sets} ${Calc.plural(summary.sets, "подход", "подхода", "подходов")}",
                    DsIcons.Weight to Calc.tonnage(summary.tonnage),
                ),
            )
        }
        Text(Calc.hms(vm.sessionElapsedSeconds()), style = DsType.Title2B.copy(fontFeatureSettings = "tnum"), color = Ds.lime)
    }
}

/** TrainingRow: дата слева, индикатор сплита, название и LabelGroup с калориями, временем и тоннажем. */
@Composable
private fun TrainingRow(vm: AppViewModel, workout: Workout) {
    val m = vm.calc.metrics(workout)
    val split = vm.calc.split(workout.splitId)
    val d = LocalDate.parse(workout.date)
    val records = vm.calc.recordEntries(workout).isNotEmpty()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { vm.sheet = Sheet.WorkoutStats(workout.id) }.padding(start = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.width(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${d.dayOfMonth}", style = DsType.Title2B, color = Ds.whiteTertiary)
            Text(Calc.monthNames[d.monthValue - 1], style = DsType.Label1R, color = Ds.whiteTertiary, modifier = Modifier.padding(top = 0.dp))
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlowDot(Bt.color(split.color))
                Spacer(Modifier.width(4.dp))
                Text(split.name, style = DsType.Title2B, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (records) Icon(DsIcons.Crown, "Личный рекорд", tint = Ds.record, modifier = Modifier.padding(start = 6.dp).size(14.dp))
            }
            MetaRow(listOf(DsIcons.Flame to "${Calc.grouped(m.calories.toLong())} ккал", DsIcons.Clock to Calc.duration(m.durationSeconds), DsIcons.Weight to Calc.tonnage(m.tonnage)))
        }
    }
}

// ======================= Статистика тренировки =======================

@Composable
fun WorkoutStatsSheet(vm: AppViewModel, sheet: Sheet.WorkoutStats) {
    val workout = vm.state.workouts.firstOrNull { it.id == sheet.workoutId }
    if (workout == null) { vm.closeSheet(); return }
    val split = vm.calc.split(workout.splitId)
    val m = vm.calc.metrics(workout)
    var menu by remember { mutableStateOf(false) }
    BtSheet(onDismiss = { vm.closeSheet() }) {
        if (sheet.completion) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(CardShape).background(Ds.lime.copy(alpha = 0.12f)).border(1.dp, Ds.limeSecondary, CardShape).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(DsIcons.Crown, null, tint = Ds.lime, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Тренировка сохранена. Отличная работа!", style = DsType.Body2B, color = Ds.lime)
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlowDot(Bt.color(split.color))
                    Spacer(Modifier.width(6.dp))
                    Text("${Calc.journalDateLabel(workout.date)}, ${LocalDate.parse(workout.date).year}", style = DsType.Body2R, color = Ds.whiteSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(split.name, style = DsType.Headline1B, modifier = Modifier.weight(1f, fill = false))
                    Box {
                        Icon(DsIcons.More, "Действия с тренировкой", tint = Ds.whiteSecondary, modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(8.dp)).clickable { menu = true }.padding(4.dp).size(20.dp))
                        DropdownMenu(menu, onDismissRequest = { menu = false }, containerColor = Ds.sheet, modifier = Modifier.border(1.dp, Ds.whiteOpacity, RoundedCornerShape(4.dp))) {
                            DropdownMenuItem(text = { Text("Редактировать", style = DsType.Body3R) }, onClick = { menu = false; vm.openWorkoutEditor(workout.id) })
                            DropdownMenuItem(text = { Text("Удалить", style = DsType.Body3R, color = Ds.negative) }, onClick = { menu = false; vm.closeSheet(); vm.deleteWorkout(workout.id) })
                        }
                    }
                }
            }
            RoundIconButton(DsIcons.Close, "Закрыть", vm.feedback, tint = Ds.whiteSecondary, background = Color.Transparent, size = 32.dp) { vm.closeSheet() }
        }
        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TrendTile(vm, workout, DsIcons.Flame, "Калории", Calc.grouped(m.calories.toLong()), "ккал") { it.calories }
                TrendTile(vm, workout, DsIcons.Weight, "Тоннаж", Calc.grouped(m.tonnage), "кг") { it.tonnage }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TrendTile(vm, workout, DsIcons.Timer, "Время тренировки", "${Math.round(m.durationSeconds / 60.0)}", "мин", neutral = true) { it.durationSeconds }
                TrendTile(vm, workout, DsIcons.Exercise, "Подходы", "${m.sets}", null) { it.sets.toLong() }
            }
        }
        WorkoutDetails(vm, workout)
        if (sheet.completion) Btn("В журнал", vm.feedback, Modifier.fillMaxWidth().padding(top = 20.dp)) { vm.closeSheet() }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TrendTile(
    vm: AppViewModel, workout: Workout, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, unit: String?,
    neutral: Boolean = false, metric: (Calc.Metrics) -> Long,
) {
    val trend = vm.calc.trend(workout, metric)
    // Рост времени тренировки — не «улучшение», поэтому для него тренд нейтрального цвета.
    val color = when {
        trend == null || neutral -> Ds.whiteTertiary
        trend.first -> Ds.lime
        else -> Ds.negative
    }
    KpiTile(icon, label, value, Modifier.weight(1f), unit = unit, trend = trend?.let { "${it.second}%" }, trendColor = color, trendUp = trend?.first)
}

/** Упражнения тренировки: заголовок Title1B + LabelGroup и таблица подходов (RepTable / TableRow). */
@Composable
private fun WorkoutDetails(vm: AppViewModel, workout: Workout) {
    val records = vm.calc.recordEntries(workout)
    @Composable
    fun exerciseBlock(exerciseId: String?, entries: List<Entry>, letter: String?) {
        val exercise = vm.calc.exercise(exerciseId)
        val summary = vm.calc.summary(entries)
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (letter != null) { SupersetLetter(letter); Spacer(Modifier.width(8.dp)) }
                Column {
                    Text(exercise.name, style = DsType.Title1B)
                    MetaRow(listOf(null to "${summary.sets} ${Calc.plural(summary.sets, "подход", "подхода", "подходов")}", null to Calc.tonnage(summary.tonnage)))
                }
            }
            Spacer(Modifier.height(8.dp))
            RepTable(entries.sortedBy { it.set }, { vm.calc.formatEntryWeight(it) }, { it in records })
        }
    }
    // Блоки в порядке выполнения: суперсет — одна группа (метка, лаймовая линия, буквы A/B), остальные — по упражнению.
    val strength = workout.entries.filter { !it.isCardio }
    val blocks = strength.map { it.supersetId?.let { s -> "superset:$s" } ?: "exercise:${it.exerciseId}" }.distinct()
    blocks.forEach { key ->
        if (key.startsWith("superset:")) {
            val group = strength.filter { it.supersetId == key.removePrefix("superset:") }
            val byExercise = group.groupBy { it.exerciseId }
            val rounds = byExercise.values.maxOf { it.size }
            Row(Modifier.padding(top = 18.dp).height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(Ds.limeSecondary))
                Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SupersetBadge()
                        Spacer(Modifier.width(8.dp))
                        Text("$rounds ${Calc.plural(rounds, "круг", "круга", "кругов")} · ${Calc.tonnage(vm.calc.summary(group).tonnage)}", style = DsType.Label2R, color = Ds.whiteTertiary)
                    }
                    byExercise.entries.forEachIndexed { index, (exerciseId, entries) -> exerciseBlock(exerciseId, entries, ('A' + index).toString()) }
                }
            }
        } else {
            val exerciseId = key.removePrefix("exercise:").takeIf { it != "null" }
            Box(Modifier.padding(top = 18.dp)) { exerciseBlock(exerciseId, strength.filter { it.supersetId == null && it.exerciseId == exerciseId }, null) }
        }
    }
    workout.entries.filter { it.isCardio }.forEach { entry ->
        Column(Modifier.padding(top = 18.dp)) {
            Text(entry.cardioName ?: "Кардио", style = DsType.Title1B)
            MetaRow(listOf(DsIcons.Clock to "${Calc.num(entry.minutes)} мин", null to (if (entry.distance > 0) "${Calc.num(entry.distance)} км" else "—"), DsIcons.Flame to "${Math.round(vm.calc.entryCalories(entry))} ккал") + if (entry.avgHr > 0) listOf(DsIcons.Heart to "${entry.avgHr} уд/мин") else emptyList())
        }
    }
}

/** RepTable: строки 28dp, чередование Black/Quinternary и Black/Opacity, корона у рекорда. */
@Composable
fun RepTable(entries: List<Entry>, weight: (Entry) -> String, isRecord: (Entry) -> Boolean, onDelete: ((Entry) -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
        entries.forEachIndexed { index, entry ->
            Row(
                Modifier.fillMaxWidth().height(if (onDelete != null) 36.dp else 28.dp)
                    .background(if (index % 2 == 0) Ds.blackQuinternary else Ds.blackOpacity)
                    .padding(start = 16.dp, end = if (onDelete != null) 4.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${index + 1}", style = DsType.Label1R, color = Ds.whiteQuaternary, modifier = Modifier.width(22.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    if (isRecord(entry)) Icon(DsIcons.Crown, "Рекорд", tint = Ds.record, modifier = Modifier.padding(end = 6.dp).size(12.dp))
                    Text(weight(entry), style = DsType.Body2R, color = if (isRecord(entry)) Ds.white else Ds.whiteSecondary)
                }
                Text("x${entry.reps}", style = DsType.Body2R, color = Ds.whiteSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                if (onDelete != null) RoundIconButton(DsIcons.Close, "Удалить подход", null, tint = Ds.whiteQuaternary, background = Color.Transparent, size = 32.dp) { onDelete(entry) }
            }
        }
    }
}
