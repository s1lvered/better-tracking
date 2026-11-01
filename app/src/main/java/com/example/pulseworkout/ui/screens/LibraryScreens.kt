package com.example.pulseworkout.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulseworkout.data.Catalog
import com.example.pulseworkout.data.Exercise
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
import com.example.pulseworkout.ui.components.CountPill
import com.example.pulseworkout.ui.components.EmptyState
import com.example.pulseworkout.ui.components.FieldLabel
import com.example.pulseworkout.ui.components.FormActions
import com.example.pulseworkout.ui.components.GlowDot
import com.example.pulseworkout.ui.components.IconBadge
import com.example.pulseworkout.ui.components.PageTitle
import com.example.pulseworkout.ui.components.Panel
import com.example.pulseworkout.ui.components.RoundIconButton
import com.example.pulseworkout.ui.components.SectionTitle
import com.example.pulseworkout.ui.components.SheetTitle
import com.example.pulseworkout.ui.theme.Bt
import com.example.pulseworkout.ui.theme.BtType
import com.example.pulseworkout.ui.theme.Ds
import com.example.pulseworkout.ui.theme.DsIcons
import com.example.pulseworkout.ui.theme.DsType
import androidx.compose.foundation.layout.size

@Composable
private fun TitleRow(vm: AppViewModel, title: String, subtitle: String, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            PageTitle(title)
            Text(subtitle, style = DsType.Body2R, color = Ds.whiteTertiary)
        }
        Btn("Добавить", vm.feedback, height = 40.dp, icon = DsIcons.Plus, onClick = onAdd)
    }
}

// ======================= Библиотека =======================

/** Строка упражнения внутри групповой карточки. */
private data class LibraryItem(val key: String, val name: String, val subtitle: String, val onEdit: (() -> Unit)?, val onDelete: () -> Unit)

@Composable
fun LibraryScreen(vm: AppViewModel) {
    val query = Calc.searchKey(vm.exerciseQuery)
    val areaTabs = listOf("all" to "Все") + Catalog.categoryGroups.map { it.id to it.name }
    fun areaMatches(e: Exercise) = vm.exerciseAreaTab == "all" || Catalog.groupByCategory[vm.calc.exerciseCategory(e)] == vm.exerciseAreaTab
    fun matches(name: String) = query in Calc.searchKey(name)
    val active = vm.calc.activeExercises()
    val catalogIds = vm.catalogIds
    val exercises = active.filter { vm.calc.exerciseCategory(it) != "Кардио" && areaMatches(it) && matches(it.name) }
    val groups = exercises.groupBy { vm.calc.exerciseCategory(it) }
    val ordered = groups.keys.sortedWith { a, b ->
        when {
            a == Catalog.UNCATEGORIZED -> -1
            b == Catalog.UNCATEGORIZED -> 1
            else -> Catalog.categories.indexOf(a) - Catalog.categories.indexOf(b)
        }
    }
    val showCardio = vm.exerciseAreaTab == "all" || vm.exerciseAreaTab == "cardio"
    val customCardio = if (showCardio) active.filter { vm.calc.exerciseCategory(it) == "Кардио" && matches(it.name) } else emptyList()
    val cardio = if (showCardio) vm.calc.visibleCardioOptions().filter { matches(it.name) } else emptyList()

    fun itemFor(e: Exercise, category: String) = LibraryItem(
        e.id, e.name, if (e.id in catalogIds) category else "$category · своё",
        // Категорию можно менять у своих упражнений; у каталожных она задана заранее.
        onEdit = if (e.id !in catalogIds || category == Catalog.UNCATEGORIZED) ({ vm.sheet = Sheet.ExerciseEdit(e.id) }) else null,
        onDelete = { vm.deleteExercise(e.id) },
    )

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            TitleRow(vm, "Библиотека", "${active.size + vm.calc.visibleCardioOptions().size} упражнений") { vm.sheet = Sheet.ExerciseEdit(null) }
            BtTextField(vm.exerciseQuery, { vm.exerciseQuery = it }, placeholder = "Найти упражнение", leading = DsIcons.Search)
            Spacer(Modifier.height(12.dp))
            ChipTabs(areaTabs, vm.exerciseAreaTab, vm.feedback) { vm.exerciseAreaTab = it }
            if (groups[Catalog.UNCATEGORIZED].orEmpty().isNotEmpty()) {
                Panel(Modifier.padding(top = 14.dp), borderColor = Ds.negative) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(DsIcons.Settings, Ds.negative)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Есть упражнения без категории", style = DsType.Title1B)
                            Text("Нажмите на упражнение, чтобы назначить категорию", style = DsType.Label2R, color = Ds.whiteTertiary)
                        }
                    }
                }
            }
        }
        ordered.forEach { category ->
            val items = groups[category].orEmpty()
            item(key = "group-$category") {
                GroupHeader(category, items.size)
                LibraryGroup(vm, items.map { itemFor(it, category) })
            }
        }
        if (customCardio.isNotEmpty() || cardio.isNotEmpty()) item(key = "group-cardio") {
            GroupHeader("Кардио", customCardio.size + cardio.size)
            LibraryGroup(vm, customCardio.map { itemFor(it, "Кардио") } + cardio.map { a -> LibraryItem("cardio-${a.id}", a.name, "Кардио · таймер", null) { vm.deleteCardio(a.id) } })
        }
        if (ordered.isEmpty() && customCardio.isEmpty() && cardio.isEmpty()) item {
            Spacer(Modifier.height(16.dp))
            EmptyState("Ничего не найдено", "Попробуйте другое название или добавьте своё упражнение", icon = DsIcons.Search)
        }
        item { BottomSpacer() }
    }
}

@Composable
private fun GroupHeader(category: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        GlowDot(Bt.color(Catalog.categoryColor(category)), 7.dp)
        Spacer(Modifier.width(6.dp))
        Text(category, style = DsType.Title2B, modifier = Modifier.weight(1f))
        CountPill("$count")
    }
}

@Composable
private fun LibraryGroup(vm: AppViewModel, items: List<LibraryItem>) {
    Column(Modifier.fillMaxWidth().clip(CardShape).background(Ds.whiteOpacity)) {
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(Modifier.padding(start = 12.dp), thickness = 1.dp, color = Ds.whiteOpacity)
            Row(
                Modifier.fillMaxWidth().then(if (item.onEdit != null) Modifier.clickable(onClick = item.onEdit) else Modifier).padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                    Text(item.name, style = DsType.Body2R)
                    Text(item.subtitle, style = DsType.Label2R, color = Ds.whiteTertiary)
                }
                RoundIconButton(DsIcons.Close, "Удалить", vm.feedback, Ds.whiteQuaternary, Color.Transparent, size = 32.dp, onClick = item.onDelete)
            }
        }
    }
}

@Composable
fun ExerciseEditSheet(vm: AppViewModel, exerciseId: String?) {
    val editing = exerciseId?.let { id -> vm.state.exercises.firstOrNull { it.id == id } }
    val initialCategory = editing?.let { vm.calc.exerciseCategory(it) }?.takeIf { it != Catalog.UNCATEGORIZED } ?: Catalog.categoryGroups[0].categories[0]
    var name by remember { mutableStateOf("") }
    var group by remember { mutableStateOf(Catalog.groupByCategory[initialCategory] ?: Catalog.categoryGroups[0].id) }
    var category by remember { mutableStateOf(initialCategory) }
    val currentGroup = Catalog.categoryGroups.first { it.id == group }

    BtSheet(onDismiss = { vm.closeSheet() }) {
        SheetTitle(if (editing != null) "Категория упражнения" else "Новое упражнение", editing?.name)
        if (editing == null) {
            FieldLabel("Название")
            BtTextField(name, { name = it.take(45) }, placeholder = "Например: жим ногами")
            Spacer(Modifier.height(14.dp))
        }
        FieldLabel("Область")
        ChipTabs(Catalog.categoryGroups.map { it.id to it.name }, group, vm.feedback) { id ->
            group = id
            category = Catalog.categoryGroups.first { it.id == id }.categories.first()
        }
        if (group != "cardio") {
            Spacer(Modifier.height(14.dp))
            FieldLabel("Категория")
            Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                currentGroup.categories.forEach { c ->
                    ChoiceRow(c, null, Bt.color(Catalog.categoryColor(c)), selected = c == category, showChevron = false) { vm.feedback.haptic(); category = c }
                }
            }
        }
        FormActions(vm.feedback, if (editing != null) "Сохранить" else "Добавить", enabled = editing != null || name.isNotBlank(), onCancel = { vm.closeSheet() }) {
            vm.saveExercise(editing?.id, name, if (group == "cardio") "Кардио" else category)
        }
    }
}

// ======================= Сплиты =======================

private data class SplitItem(val id: String, val name: String, val category: String)

@Composable
fun SplitsScreen(vm: AppViewModel) {
    val splits = vm.calc.orderedSplits()
    val active = vm.calc.activeExercises()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            TitleRow(vm, "Сплиты", "${splits.size} ${Calc.plural(splits.size, "сплит", "сплита", "сплитов")} · структура недели") { vm.sheet = Sheet.SplitEdit(null) }
            if (splits.isEmpty()) EmptyState("Создайте первый сплит", "Например: «Грудь», «Спина», «Ноги» — и добавьте в него упражнения", icon = DsIcons.Splits)
        }
        items(splits, key = { it.id }) { split ->
            val list = active.filter { split.id in it.splitIds }
            SplitCard(
                vm, split.id, split.name,
                "${list.size} ${Calc.plural(list.size, "упражнение", "упражнения", "упражнений")}",
                Bt.color(split.color),
                list.map { SplitItem(it.id, it.name, vm.calc.categoryOf(it)) },
                readonly = false,
                onDelete = { vm.deleteSplit(split.id) },
            )
            Spacer(Modifier.height(10.dp))
        }
        item(key = "other") {
            SectionTitle("Во всех сплитах")
            val universal = active.filter { it.universal && !it.cardio }
            val cardio = vm.calc.visibleCardioOptions()
            val items = universal.map { SplitItem(it.id, it.name, vm.calc.categoryOf(it)) } + cardio.map { SplitItem(it.id, it.name, "Кардио") }
            SplitCard(vm, "other", vm.state.otherName, "Доступно в каждой тренировке · ${items.size}", Bt.color("gray"), items, readonly = true, onDelete = null)
        }
        item { BottomSpacer() }
    }
}

@Composable
private fun SplitCard(vm: AppViewModel, id: String, title: String, subtitle: String, color: Color, items: List<SplitItem>, readonly: Boolean, onDelete: (() -> Unit)?) {
    val expanded = id in vm.expandedSplits
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    Row(
        Modifier.fillMaxWidth().height(IntrinsicSize.Min).clip(CardShape).background(Ds.whiteOpacity).animateContentSize(),
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(color))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth().clickable { vm.feedback.haptic(); vm.toggleSplit(id) }.padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (id == "other") { IconBadge(DsIcons.Exercise, Ds.whiteTertiary, 34.dp); Spacer(Modifier.width(12.dp)) }
                Column(Modifier.weight(1f)) {
                    Text(title, style = DsType.Title2B)
                    Text(subtitle, style = DsType.Label2R, color = Ds.whiteTertiary)
                }
                Icon(DsIcons.ChevronDown, null, tint = Ds.whiteTertiary, modifier = Modifier.padding(8.dp).size(18.dp).rotate(arrow))
                if (onDelete != null) RoundIconButton(DsIcons.Trash, "Удалить", vm.feedback, Ds.negative, Color.Transparent, onClick = onDelete)
            }
            if (expanded) Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                val categories = vm.calc.sortCategories(items.map { it.category })
                val selected = vm.splitCategoryTabs[id]?.takeIf { it in categories } ?: "all"
                if (categories.size > 1) {
                    ChipTabs(listOf("all" to "Все") + categories.map { it to it }, selected, vm.feedback) { vm.splitCategoryTabs[id] = it }
                    Spacer(Modifier.height(4.dp))
                }
                val visible = if (selected == "all") items else items.filter { it.category == selected }
                if (visible.isEmpty()) Text("Здесь пока пусто — добавьте упражнения", style = DsType.Label2R, color = Ds.whiteTertiary, modifier = Modifier.padding(vertical = 8.dp))
                vm.calc.sortCategories(visible.map { it.category }).forEach { category ->
                    val group = visible.filter { it.category == category }
                    Row(Modifier.padding(top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlowDot(Bt.color(Catalog.categoryColor(category)), 6.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(category, style = DsType.Label2B, color = Ds.whiteSecondary, modifier = Modifier.weight(1f))
                        Text("${group.size}", style = DsType.Label2R, color = Ds.whiteTertiary)
                    }
                    Column(Modifier.clip(RoundedCornerShape(12.dp))) {
                        group.forEachIndexed { index, item ->
                            Row(
                                Modifier.fillMaxWidth().background(if (index % 2 == 0) Ds.blackQuinternary else Ds.blackOpacity).padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(item.name, style = DsType.Body2R, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
                                if (!readonly) RoundIconButton(DsIcons.Close, "Убрать из сплита", vm.feedback, Ds.whiteQuaternary, Color.Transparent, size = 32.dp) { vm.removeExerciseFromSplit(item.id, id) }
                            }
                        }
                    }
                }
                Btn("Добавить упражнения", vm.feedback, Modifier.fillMaxWidth().padding(top = 12.dp), BtnKind.Secondary, icon = DsIcons.Plus, height = 44.dp) { vm.sheet = Sheet.SplitPicker(id) }
            }
        }
    }
}

@Composable
fun SplitEditSheet(vm: AppViewModel, splitId: String?) {
    val editing = splitId?.let { id -> vm.state.splits.firstOrNull { it.id == id } }
    var name by remember { mutableStateOf(editing?.name.orEmpty()) }
    BtSheet(onDismiss = { vm.closeSheet() }) {
        SheetTitle(if (editing != null) "Изменить сплит" else "Новый сплит", if (editing == null) "После создания сразу выберете упражнения" else null)
        FieldLabel("Название")
        BtTextField(name, { name = it.take(24) }, placeholder = "Например: Full body")
        Row(Modifier.padding(top = 12.dp)) {
            listOf("Грудь", "Спина", "Ноги", "Руки", "Full body").forEach { suggestion ->
                Text(
                    suggestion, style = DsType.Label2R, color = Ds.whiteSecondary,
                    modifier = Modifier.padding(end = 6.dp).clip(RoundedCornerShape(50)).background(Ds.whiteOpacity).clickable { name = suggestion }.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        FormActions(vm.feedback, if (editing != null) "Сохранить" else "Создать", enabled = name.isNotBlank(), onCancel = { vm.closeSheet() }) {
            vm.saveSplit(editing?.id, name)
        }
    }
}

@Composable
fun SplitPickerSheet(vm: AppViewModel, splitId: String) {
    val isOther = splitId == "other"
    val label = if (isOther) vm.state.otherName else vm.calc.split(splitId).name
    val active = vm.calc.activeExercises()
    val available = if (isOther) active.filter { !it.universal && !it.cardio }
    else active.filter { !it.universal && vm.calc.exerciseCategory(it) != "Пресс и кор" && splitId !in it.splitIds }
    val byCategory = available.groupBy { vm.calc.categoryOf(it) }
    val categories = vm.calc.sortCategories(byCategory.keys)
    var tab by remember { mutableStateOf("all") }
    var query by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<String>() }

    BtSheet(onDismiss = { vm.closeSheet() }) {
        SheetTitle("Добавить в «$label»", "Можно выбрать несколько упражнений")
        BtTextField(query, { query = it }, placeholder = "Поиск", leading = DsIcons.Search)
        Spacer(Modifier.height(10.dp))
        ChipTabs(listOf("all" to "Все") + categories.map { it to it }, tab, vm.feedback) { tab = it }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
            val visibleCategories = if (tab == "all") categories else listOf(tab)
            if (available.isEmpty()) EmptyState("Все упражнения уже добавлены")
            visibleCategories.forEach { category ->
                val items = byCategory[category].orEmpty().filter { Calc.searchKey(query) in Calc.searchKey(it.name) }
                if (items.isEmpty()) return@forEach
                Row(Modifier.padding(top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlowDot(Bt.color(Catalog.categoryColor(category)), 6.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(category, style = DsType.Label2B, color = Ds.whiteSecondary, modifier = Modifier.weight(1f))
                    Text("${items.size}", style = DsType.Label2R, color = Ds.whiteTertiary)
                }
                items.forEach { exercise ->
                    val isSelected = exercise.id in selected
                    ChoiceRow(exercise.name, null, null, selected = isSelected, showChevron = false) {
                        vm.feedback.haptic()
                        if (isSelected) selected.remove(exercise.id) else selected += exercise.id
                    }
                }
            }
        }
        FormActions(vm.feedback, if (selected.isEmpty()) "Добавить" else "Добавить (${selected.size})", enabled = selected.isNotEmpty(), onCancel = { vm.closeSheet() }) {
            vm.addExercisesToSplit(splitId, selected.toSet())
        }
    }
}
