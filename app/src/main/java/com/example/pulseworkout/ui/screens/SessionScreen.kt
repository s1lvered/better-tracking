package com.example.pulseworkout.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pulseworkout.data.Catalog
import com.example.pulseworkout.data.Entry
import com.example.pulseworkout.data.ExerciseInstance
import com.example.pulseworkout.logic.Calc
import com.example.pulseworkout.ui.AppViewModel
import com.example.pulseworkout.ui.BottomSpacer
import com.example.pulseworkout.ui.Sheet
import com.example.pulseworkout.ui.components.Btn
import com.example.pulseworkout.ui.components.BtSheet
import com.example.pulseworkout.ui.components.BtTextField
import com.example.pulseworkout.ui.components.BtnKind
import com.example.pulseworkout.ui.components.CardShape
import com.example.pulseworkout.ui.components.ChipTabs
import com.example.pulseworkout.ui.components.ChoiceRow
import com.example.pulseworkout.ui.components.EmptyState
import com.example.pulseworkout.ui.components.Eyebrow
import com.example.pulseworkout.ui.components.FieldLabel
import com.example.pulseworkout.ui.components.FormActions
import com.example.pulseworkout.ui.components.GlowDot
import com.example.pulseworkout.ui.components.IconBadge
import com.example.pulseworkout.ui.components.KpiTile
import com.example.pulseworkout.ui.components.MetaRow
import com.example.pulseworkout.ui.components.Panel
import com.example.pulseworkout.ui.components.PillShape
import com.example.pulseworkout.ui.components.Ring
import com.example.pulseworkout.ui.components.RoundIconButton
import com.example.pulseworkout.ui.components.SectionTitle
import com.example.pulseworkout.ui.components.SheetTitle
import com.example.pulseworkout.ui.components.WheelPicker
import com.example.pulseworkout.ui.theme.Bt
import com.example.pulseworkout.ui.theme.BtType
import com.example.pulseworkout.ui.theme.Ds
import com.example.pulseworkout.ui.theme.DsIcons
import com.example.pulseworkout.ui.theme.DsType
import kotlin.math.roundToInt

@Composable
fun ActiveSessionScreen(vm: AppViewModel) {
    val s = vm.session ?: return
    val editing = s.editingWorkoutId != null
    val split = vm.calc.split(s.splitId)
    val wearable by vm.wearable.state.collectAsState()
    val connected = wearable.connected != null
    val sum = vm.calc.summary(s.entries)
    val calories = s.entries.sumOf { vm.calc.entryCalories(it) }.roundToInt()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlowDot(Bt.color(split.color))
            Spacer(Modifier.width(6.dp))
            Text(if (editing) "Редактирование · ${Calc.shortDate(s.date)}" else "${Calc.journalDateLabel(s.date)} · ${split.name}", style = DsType.Body2R, color = Ds.whiteTertiary)
        }
        Text(if (editing) "Изменить тренировку" else "Тренировка", style = DsType.Headline1B)
        Spacer(Modifier.height(16.dp))

        if (!editing) {
            TimerHero(vm, connected, wearable.bpm)
            Spacer(Modifier.height(10.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiTile(DsIcons.Weight, "Тоннаж", Calc.grouped(sum.tonnage), Modifier.weight(1f), unit = "кг")
            KpiTile(DsIcons.Exercise, "Подходы", "${sum.sets}", Modifier.weight(1f))
            KpiTile(DsIcons.Flame, "Калории", "$calories", Modifier.weight(1f), unit = "ккал")
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Btn("Добавить упражнение", vm.feedback, Modifier.weight(1f), icon = DsIcons.Plus) { vm.sheet = Sheet.ExerciseChooser }
            if (!editing) {
                RoundIconButton(
                    DsIcons.Radiowaves, if (connected) "Отключить браслет" else "Подключить браслет", vm.feedback,
                    tint = if (connected) Ds.black else Ds.white, background = if (connected) Ds.lime else Ds.whiteOpacity, size = 52.dp,
                ) { vm.toggleWearable() }
            }
        }

        val cardio = s.entries.filter { it.isCardio }
        val groups = s.exerciseInstances.map { instance -> instance to vm.instanceEntries(instance) }
            .filter { (instance, entries) -> entries.isNotEmpty() || !instance.completed }
        if (cardio.isNotEmpty() || groups.isNotEmpty()) SectionTitle("Упражнения", trailing = "${cardio.size + groups.size}")
        else EmptyState("Добавьте первое упражнение", "Выберите движение из сплита\nили запустите кардио", icon = DsIcons.Exercise)
        cardio.forEach { CardioCard(vm, it, connected, wearable.bpm); Spacer(Modifier.height(10.dp)) }
        groups.forEach { (instance, entries) -> ExerciseCard(vm, instance, entries); Spacer(Modifier.height(10.dp)) }

        Spacer(Modifier.height(8.dp))
        Btn(
            if (editing) "Сохранить изменения" else "Завершить тренировку", vm.feedback, Modifier.fillMaxWidth(),
            kind = if (editing) BtnKind.Primary else BtnKind.Danger, icon = if (editing) DsIcons.Check else DsIcons.StopFill,
        ) { vm.requestFinishSession() }
        BottomSpacer()
    }
}

/** TimerHero: общее время Display1B, пульс и кольцо отдыха (заполняется за минуту). */
@Composable
private fun TimerHero(vm: AppViewModel, connected: Boolean, bpm: Int?) {
    val rest = vm.restElapsedSeconds()
    val running = vm.restStartedAt > 0
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(Ds.blackTertiary).border(1.dp, Ds.borderBrush, shape).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(DsIcons.Clock, null, tint = Ds.whiteTertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Время тренировки", style = DsType.Label2R, color = Ds.whiteTertiary)
            }
            Text(Calc.clock(vm.sessionElapsedSeconds()), style = BtType.timer)
            if (connected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(DsIcons.Heart, null, tint = Ds.negative, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${bpm ?: "—"} уд/мин", style = DsType.Label2B, color = Ds.whiteSecondary)
                    Text(" · ср. ${vm.sessionAverageHr() ?: "—"}", style = DsType.Label2R, color = Ds.whiteTertiary)
                }
            }
        }
        Box(contentAlignment = Alignment.Center) {
            Ring(if (running) (rest % 60) / 60f else 0f, Modifier.size(104.dp), color = if (running) Ds.lime else Ds.whiteQuaternary)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Calc.clock(rest), style = DsType.Title3B.copy(fontFeatureSettings = "tnum"), color = if (running) Ds.white else Ds.whiteSecondary)
                Text("отдых", style = DsType.Label1R, color = Ds.whiteTertiary)
                Spacer(Modifier.height(2.dp))
                RoundIconButton(
                    if (running) DsIcons.PauseFill else DsIcons.PlayFill, if (running) "Стоп" else "Старт", vm.feedback,
                    tint = Ds.black, background = Ds.lime, size = 26.dp,
                ) { if (running) vm.stopRest() else vm.startRest() }
            }
        }
    }
}

/** CardioCard: иконка сердца на красной подложке, название и LabelGroup. */
@Composable
private fun CardioCard(vm: AppViewModel, entry: Entry, connected: Boolean, bpm: Int?) {
    if (entry.isRunningCardio) {
        Panel(brush = Bt.heroBrush, borderColor = Ds.lime) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(DsIcons.Heart, Ds.negative)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Кардио идёт", style = DsType.Label2R, color = Ds.lime)
                    Text(entry.cardioName.orEmpty(), style = DsType.Title1B)
                }
                Text(Calc.clock(vm.calc.cardioElapsedSeconds(entry, vm.now)), style = DsType.Headline1B.copy(fontFeatureSettings = "tnum"), color = Ds.lime)
            }
            if (connected) {
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiTile(DsIcons.Heart, "Текущий", "${bpm ?: "—"}", Modifier.weight(1f), unit = "уд/мин")
                    KpiTile(DsIcons.Heart, "Средний", "${vm.calc.cardioAverageHr(entry).takeIf { it > 0 } ?: "—"}", Modifier.weight(1f), unit = "уд/мин")
                }
            }
            Btn("Завершить кардио", vm.feedback, Modifier.fillMaxWidth().padding(top = 12.dp), BtnKind.Secondary, height = 44.dp) { vm.openCardioFinish(entry.cardioEntryId!!) }
        }
    } else {
        val duration = if (entry.durationSeconds > 0) entry.durationSeconds else (entry.minutes * 60).roundToInt().toLong()
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(DsIcons.Heart, Ds.negative)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.cardioName.orEmpty(), style = DsType.Title1B)
                    MetaRow(
                        listOfNotNull(
                            DsIcons.Clock to Calc.clock(duration),
                            if (entry.distance > 0) null to "${Calc.num(entry.distance)} км" else null,
                            DsIcons.Flame to "${vm.calc.entryCalories(entry).roundToInt()} ккал",
                            if (entry.avgHr > 0) DsIcons.Heart to "${entry.avgHr}" else null,
                        ),
                    )
                }
            }
        }
    }
}

/** SessionExerciseCard: заголовок, таблица подходов с удалением, «Подход» и «Завершить». */
@Composable
private fun ExerciseCard(vm: AppViewModel, instance: ExerciseInstance, entries: List<Entry>) {
    val exercise = vm.calc.exercise(instance.exerciseId)
    val category = vm.calc.categoryOf(exercise)
    val summary = vm.calc.summary(entries)
    Panel(Modifier.animateContentSize(), borderColor = if (instance.completed) androidx.compose.ui.graphics.Color.Transparent else Ds.limeSecondary) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlowDot(Bt.color(Catalog.categoryColor(category)))
                    Spacer(Modifier.width(6.dp))
                    Text(exercise.name, style = DsType.Title1B, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                MetaRow(
                    if (instance.completed) listOf(DsIcons.Check to "Завершено", null to "${summary.sets} ${Calc.plural(summary.sets, "подход", "подхода", "подходов")}", null to Calc.tonnage(summary.tonnage))
                    else listOf(null to category) + if (summary.sets > 0) listOf(null to "${summary.sets} ${Calc.plural(summary.sets, "подход", "подхода", "подходов")}", null to Calc.tonnage(summary.tonnage)) else emptyList(),
                )
            }
            if (instance.completed) Text("Изменить", style = DsType.Label3B, color = Ds.lime, modifier = Modifier.clip(PillShape).clickable { vm.feedback.haptic(); vm.reopenExercise(instance.instanceId) }.padding(6.dp))
        }
        if (instance.completed) return@Panel
        Spacer(Modifier.height(10.dp))
        if (entries.isEmpty()) {
            Text("Подходов пока нет — добавьте первый", style = DsType.Label2R, color = Ds.whiteTertiary, modifier = Modifier.padding(vertical = 4.dp))
        } else {
            RepTable(entries, { vm.calc.formatEntryWeight(it) }, { false }, onDelete = { vm.feedback.haptic(); vm.deleteSet(it) })
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Подход", vm.feedback, Modifier.weight(1f), BtnKind.Secondary, icon = DsIcons.Plus, height = 44.dp) { vm.openSetEntry(instance.exerciseId, instance.instanceId) }
            Btn("Завершить", vm.feedback, Modifier.weight(1f), BtnKind.Text, icon = DsIcons.Check, height = 44.dp) { vm.finishExercise(instance.instanceId) }
        }
    }
}

// ======================= Шторки =======================

@Composable
fun StartWorkoutSheet(vm: AppViewModel) {
    val splits = vm.calc.orderedSplits()
    var selected by remember { mutableStateOf(splits.firstOrNull()?.id) }
    BtSheet(onDismiss = { vm.closeSheet() }) {
        SheetTitle("Начать тренировку", "Выберите сплит — упражнения подтянутся из него")
        if (splits.isEmpty()) {
            EmptyState("Сплитов пока нет", "Создайте сплит в разделе «Сплиты»", icon = DsIcons.Splits) {
                Btn("Перейти к сплитам", vm.feedback) { vm.closeSheet(); vm.tab = "splits" }
            }
            return@BtSheet
        }
        Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
            splits.forEach { split ->
                val count = vm.calc.activeExercises().count { split.id in it.splitIds }
                ChoiceRow(split.name, "$count ${Calc.plural(count, "упражнение", "упражнения", "упражнений")}", Bt.color(split.color), selected = selected == split.id, showChevron = false, checkable = true) {
                    vm.feedback.haptic()
                    selected = split.id
                }
            }
        }
        FormActions(vm.feedback, "Старт", enabled = selected != null, onCancel = { vm.closeSheet() }) {
            selected?.let { vm.startSession(it) }
        }
    }
}

@Composable
fun ExerciseChooserSheet(vm: AppViewModel) {
    val s = vm.session ?: return
    val active = vm.calc.activeExercises()
    val splitExercises = active.filter { !it.universal && s.splitId in it.splitIds }
    val splitIds = splitExercises.map { it.id }.toSet()
    val strength = splitExercises + active.filter { it.universal && it.id !in splitIds }
    val categories = vm.calc.sortCategories(strength.map { vm.calc.categoryOf(it) })
    val tabs = listOf("all" to "Все") + categories.map { "category:$it" to it } + ("cardio" to "Кардио")
    var tab by remember { mutableStateOf("all") }
    BtSheet(onDismiss = { vm.closeSheet() }) {
        SheetTitle("Добавить упражнение")
        ChipTabs(tabs, tab, vm.feedback) { tab = it }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
            if (tab == "cardio") {
                vm.calc.visibleCardioOptions().forEach { activity ->
                    ChoiceRow(activity.name, if (s.editingWorkoutId != null) "Ввести вручную" else "Запустить таймер", Ds.negative) { vm.startCardio(activity.id) }
                }
            } else {
                val list = if (tab.startsWith("category:")) strength.filter { vm.calc.categoryOf(it) == tab.removePrefix("category:") } else strength
                if (list.isEmpty()) EmptyState("В сплите нет упражнений", "Добавьте их в разделе «Сплиты»", icon = DsIcons.Exercise)
                list.forEach { exercise ->
                    val category = vm.calc.categoryOf(exercise)
                    val inSession = s.exerciseInstances.any { it.exerciseId == exercise.id }
                    ChoiceRow(exercise.name, if (inSession) "$category · уже в тренировке" else category, Bt.color(Catalog.categoryColor(category))) {
                        vm.selectSessionExercise(exercise.id)
                    }
                }
            }
        }
    }
}

@Composable
fun SetEntrySheet(vm: AppViewModel, sheet: Sheet.SetEntry) {
    val exercise = vm.calc.exercise(sheet.exerciseId)
    val mode = vm.calc.loadMode(exercise)
    val defaults = remember(sheet) { vm.setDefaults(sheet.exerciseId, sheet.instanceId) }
    val weights = remember(sheet) { vm.weightOptions(exercise) }
    val reps = remember { (1..100).toList() }
    val supportsBodyweight = exercise.bodyweight?.supported == true
    val canToggle = exercise.bodyweight?.weighted == true
    var weighted by remember(sheet) { mutableStateOf(defaults.weighted) }
    var weight by remember(sheet) { mutableDoubleStateOf(defaults.weight) }
    var repCount by remember(sheet) { mutableIntStateOf(defaults.reps) }
    val weightIndex = remember(sheet) {
        weights.indexOfFirst { kotlin.math.abs(it - defaults.weight) < 0.001 }.takeIf { it >= 0 }
            ?: weights.indices.minByOrNull { kotlin.math.abs(weights[it] - defaults.weight) } ?: 0
    }
    val showWeight = weighted || !supportsBodyweight

    BtSheet(onDismiss = { vm.closeSheet() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlowDot()
            Spacer(Modifier.width(6.dp))
            Text("Подход ${defaults.setIndex + 1}", style = DsType.Body2R, color = Ds.whiteSecondary)
        }
        Text(exercise.name, style = DsType.Headline1B)
        if (canToggle) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).height(48.dp).clip(CardShape).background(Ds.blackQuaternary).border(0.5.dp, Ds.whiteQuinternary, CardShape).padding(start = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("С утяжелением", style = DsType.Body2R, modifier = Modifier.weight(1f))
                Switch(weighted, { vm.feedback.haptic(); weighted = it }, colors = SwitchDefaults.colors(checkedTrackColor = Ds.lime, checkedThumbColor = Ds.black, uncheckedTrackColor = Ds.whiteOpacity, uncheckedBorderColor = Ds.whiteQuinternary, uncheckedThumbColor = Ds.whiteTertiary))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (showWeight) Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(when (mode) { "assisted" -> "Помощь"; "paired-dumbbell" -> "Вес на руку"; else -> "Вес" }, style = DsType.Label2R, color = Ds.whiteTertiary)
                WheelPicker(weights, weightIndex, { v -> if (v == 0.0) (if (mode == "assisted") "Без помощи" else "Без веса") else "${Calc.num(v)} кг" }, vm.feedback) { weight = it }
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Повторения", style = DsType.Label2R, color = Ds.whiteTertiary)
                WheelPicker(reps, defaults.reps - 1, { "$it" }, vm.feedback) { repCount = it }
            }
        }
        FormActions(vm.feedback, "Добавить подход", onCancel = { vm.closeSheet() }) {
            vm.addSet(sheet.exerciseId, sheet.instanceId, weight, repCount, weighted)
        }
    }
}

@Composable
fun CardioFinishSheet(vm: AppViewModel, sheet: Sheet.CardioFinish) {
    val entry = vm.session?.entries?.firstOrNull { it.cardioEntryId == sheet.cardioEntryId && it.completed == false }
    if (entry == null) { vm.closeSheet(); return }
    val activity = vm.calc.cardioOption(entry.cardioId)
    val durationSeconds = maxOf(1L, (sheet.requestedAt - (entry.startedAt ?: sheet.requestedAt)) / 1000)
    val connected = vm.wearable.state.value.connected != null
    val detected = remember(sheet) { if (connected) vm.calc.cardioAverageHr(entry).takeIf { it > 0 } ?: vm.wearable.state.value.bpm ?: 0 else 0 }
    var distance by remember(sheet) { mutableStateOf("") }
    var resistance by remember(sheet) { mutableStateOf("") }
    var hr by remember(sheet) { mutableStateOf(if (detected > 0) "$detected" else "") }
    val d = distance.replace(',', '.').toDoubleOrNull() ?: 0.0
    val r = resistance.replace(',', '.').toDoubleOrNull() ?: 0.0
    val h = hr.toIntOrNull() ?: 0
    val speed = if (d > 0) d / (durationSeconds / 3600.0) else 0.0
    val calories = vm.calc.cardioCalories(activity, durationSeconds / 60.0, speed, 0.0, r, h)

    BtSheet(onDismiss = { vm.closeSheet() }) {
        Eyebrow("Длительность ${Calc.clock(durationSeconds)}", Ds.lime)
        SheetTitle("Завершить «${entry.cardioName}»")
        CardioFields(distance, { distance = it }, resistance, { resistance = it }, hr, { hr = it.filter(Char::isDigit).take(3) }, if (connected) "Заполнено по данным браслета" else "Можно ввести вручную")
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiTile(DsIcons.Timer, "Средняя скорость", if (speed > 0) "%.1f".format(speed) else "0", Modifier.weight(1f), unit = "км/ч")
            KpiTile(DsIcons.Flame, "Калории", "${calories.roundToInt()}", Modifier.weight(1f), unit = "ккал")
        }
        FormActions(vm.feedback, "Завершить кардио", onCancel = { vm.closeSheet() }) {
            vm.finishCardio(entry.cardioEntryId!!, sheet.requestedAt, d, r, h)
        }
    }
}

@Composable
fun CardioManualSheet(vm: AppViewModel, sheet: Sheet.CardioManual) {
    val activity = vm.calc.cardioOption(sheet.activityId)
    var minutes by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var resistance by remember { mutableStateOf("") }
    var hr by remember { mutableStateOf("") }
    val m = minutes.replace(',', '.').toDoubleOrNull() ?: 0.0
    BtSheet(onDismiss = { vm.closeSheet() }) {
        Eyebrow("Кардио", Ds.lime)
        SheetTitle(activity.name, "Для прошедшей тренировки данные вводятся вручную")
        FieldLabel("Длительность")
        BtTextField(minutes, { minutes = it.take(5) }, placeholder = "20", number = true, suffix = "мин")
        Spacer(Modifier.height(12.dp))
        CardioFields(distance, { distance = it }, resistance, { resistance = it }, hr, { hr = it.filter(Char::isDigit).take(3) }, null)
        FormActions(vm.feedback, "Добавить", enabled = m > 0, onCancel = { vm.closeSheet() }) {
            vm.addManualCardio(activity.id, m, distance.replace(',', '.').toDoubleOrNull() ?: 0.0, resistance.replace(',', '.').toDoubleOrNull() ?: 0.0, hr.toIntOrNull() ?: 0)
        }
    }
}

@Composable
private fun CardioFields(distance: String, onDistance: (String) -> Unit, resistance: String, onResistance: (String) -> Unit, hr: String, onHr: (String) -> Unit, hrHint: String?) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) { FieldLabel("Дистанция"); BtTextField(distance, { onDistance(it.take(6)) }, placeholder = "0", number = true, suffix = "км") }
        Column(Modifier.weight(1f)) { FieldLabel("Сопротивление"); BtTextField(resistance, { onResistance(it.take(5)) }, placeholder = "0", number = true) }
    }
    Spacer(Modifier.height(12.dp))
    FieldLabel("Средний пульс")
    BtTextField(hr, onHr, placeholder = "Например, 142", number = true, suffix = "уд/мин", leading = DsIcons.Heart)
    if (hrHint != null) Text(hrHint, style = DsType.Label2R, color = Ds.whiteTertiary, modifier = Modifier.padding(top = 4.dp))
}
