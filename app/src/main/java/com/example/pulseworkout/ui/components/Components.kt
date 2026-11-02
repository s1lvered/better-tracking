package com.example.pulseworkout.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.example.pulseworkout.ui.Feedback
import com.example.pulseworkout.ui.theme.Bt
import com.example.pulseworkout.ui.theme.BtType
import com.example.pulseworkout.ui.theme.Ds
import com.example.pulseworkout.ui.theme.DsIcons
import com.example.pulseworkout.ui.theme.DsType
import kotlinx.coroutines.flow.distinctUntilChanged

// Радиусы дизайн-системы: карточки, плитки и поля — 12, кнопки и таб-бар — капсула.
val CardShape = RoundedCornerShape(12.dp)
val TileShape = RoundedCornerShape(12.dp)
val FieldShape = RoundedCornerShape(12.dp)
val PillShape = RoundedCornerShape(50)

// ---------------- примитивы ----------------

@Composable
fun Dot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

/** Indicator: точка 5dp с мягким радиальным ореолом 8dp. */
@Composable
fun GlowDot(color: Color = Ds.lime, size: Dp = 8.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Brush.radialGradient(listOf(color.copy(alpha = 0.5f), Color.Transparent))),
        contentAlignment = Alignment.Center,
    ) { Dot(color, size * 0.625f) }
}

@Composable
fun Eyebrow(text: String, color: Color = Ds.whiteTertiary) {
    Text(text, style = DsType.Label2R, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun PageTitle(text: String) {
    Text(text, style = DsType.Headline1B)
}

/** SectionHeader: заголовок Title2B, счётчик Label2R и необязательное действие справа. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = DsType.Title2B)
        if (trailing != null) Text(trailing, style = DsType.Label2R, color = Ds.whiteTertiary, modifier = Modifier.padding(start = 6.dp))
        Spacer(Modifier.weight(1f))
        if (action != null && onAction != null) Text(action, style = DsType.Label3B, color = Ds.lime, modifier = Modifier.clip(PillShape).clickable(onClick = onAction).padding(4.dp))
    }
}

@Composable
fun CountPill(text: String, color: Color = Ds.whiteTertiary) {
    Text(text, color = color, style = DsType.Label2R, modifier = Modifier.clip(PillShape).background(Ds.whiteOpacity).padding(horizontal = 8.dp, vertical = 2.dp))
}

/** Иконка на круглой полупрозрачной подложке её цвета (как в CardioCard). */
@Composable
fun IconBadge(icon: ImageVector, tint: Color = Ds.lime, size: Dp = 36.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(tint.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/** LabelGroup: подписи с иконками, разделённые точками 2dp. */
@Composable
fun MetaRow(items: List<Pair<ImageVector?, String>>, modifier: Modifier = Modifier, color: Color = Ds.whiteTertiary) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, (icon, text) ->
            if (index > 0) Box(Modifier.padding(horizontal = 6.dp).size(2.dp).clip(CircleShape).background(Ds.whiteQuinternary))
            if (icon != null) {
                Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(2.dp))
            }
            Text(text, style = DsType.Label2R, color = color, maxLines = 1)
        }
    }
}

// ---------------- карточки ----------------

/** Карточка: White/Opacity, радиус 12. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    brush: Brush = Bt.cardBrush,
    borderColor: Color = Color.Transparent,
    padding: PaddingValues = PaddingValues(12.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(brush)
            .then(if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, CardShape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Список без подложки: строки разделены линиями White/Opacity, как история в журнале. */
@Composable
fun ListGroup(modifier: Modifier = Modifier, rows: List<@Composable () -> Unit>) {
    Column(modifier.fillMaxWidth()) {
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = Ds.whiteOpacity)
            row()
        }
    }
}

/** StatsTab: подпись с иконкой, значение Title2B с единицей и тренд со стрелкой справа. */
@Composable
fun KpiTile(
    icon: ImageVector?,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    @Suppress("UNUSED_PARAMETER") tint: Color = Ds.whiteTertiary,
    trend: String? = null,
    trendColor: Color = Ds.whiteTertiary,
    caption: String? = null,
    trendUp: Boolean? = null,
) {
    Column(modifier.clip(TileShape).background(Ds.whiteOpacity).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = Ds.whiteTertiary, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(2.dp))
            }
            Text(label, style = DsType.Label2R, color = Ds.whiteTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = BtType.number, maxLines = 1)
            if (unit != null) Text(" $unit", style = DsType.Label2R, modifier = Modifier.padding(bottom = 3.dp))
            Spacer(Modifier.weight(1f))
            if (trend != null) Row(Modifier.padding(bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                if (trendUp != null) Icon(if (trendUp) DsIcons.TrendUp else DsIcons.TrendDown, null, tint = trendColor, modifier = Modifier.size(12.dp))
                Text(trend, style = DsType.Label2R, color = trendColor, maxLines = 1)
            }
        }
        if (caption != null) Text(caption, style = DsType.Label1R, color = Ds.whiteTertiary)
    }
}

/** Компактная метрика (StatsTab без иконки). */
@Composable
fun MetricCell(value: String, label: String, modifier: Modifier = Modifier, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Column(modifier.clip(TileShape).background(Ds.whiteOpacity).padding(horizontal = 8.dp, vertical = 6.dp)) {
        Text(label, style = DsType.Label2R, color = Ds.whiteTertiary, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = BtType.number, maxLines = 1, overflow = TextOverflow.Ellipsis)
            trailing?.invoke(this)
        }
    }
}

@Composable
fun TrendArrow(down: Boolean?) {
    if (down == null) return
    Icon(if (down) DsIcons.TrendDown else DsIcons.TrendUp, null, tint = if (down) Ds.lime else Ds.negative, modifier = Modifier.padding(start = 4.dp).size(14.dp))
}

// ---------------- кнопки ----------------

enum class BtnKind { Primary, Secondary, Danger, Text, Ghost }

/** Button: Primary — капсула с лаймовым градиентом, Plain (Text) — лаймовый текст. */
@Composable
fun Btn(
    text: String,
    feedback: Feedback?,
    modifier: Modifier = Modifier,
    kind: BtnKind = BtnKind.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 52.dp,
    @Suppress("UNUSED_PARAMETER") fontSize: TextUnit = TextUnit.Unspecified,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, spring(stiffness = 900f), label = "press")
    val background: Brush = when {
        !enabled && (kind == BtnKind.Primary || kind == BtnKind.Secondary) -> SolidColor(Ds.whiteQuinternary)
        kind == BtnKind.Primary -> Ds.primaryBrush
        kind == BtnKind.Secondary -> SolidColor(Ds.whiteOpacity)
        kind == BtnKind.Danger -> SolidColor(Ds.negative.copy(alpha = 0.12f))
        else -> SolidColor(Color.Transparent)
    }
    val fg = when {
        !enabled -> Ds.whiteQuaternary
        kind == BtnKind.Primary -> Ds.black
        kind == BtnKind.Danger -> Ds.negative
        kind == BtnKind.Text -> Ds.lime
        else -> Ds.white
    }
    val plain = kind == BtnKind.Text || kind == BtnKind.Ghost
    Row(
        modifier
            .scale(scale)
            .heightIn(min = if (plain) 36.dp else height)
            .clip(PillShape)
            .background(background)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                feedback?.haptic(if (kind == BtnKind.Danger) 14 else 8)
                onClick()
            }
            .padding(horizontal = if (plain) 12.dp else 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = fg, style = DsType.Label3B, textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** IconButton: круглая кнопка с иконкой на White/Opacity. */
@Composable
fun RoundIconButton(icon: ImageVector, description: String, feedback: Feedback?, tint: Color = Ds.whiteSecondary, background: Color = Ds.whiteOpacity, size: Dp = 36.dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).background(background).clickable { feedback?.haptic(); onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

// ---------------- выбор ----------------

/** Chip: выбранный — Lime/Accent с тёмным текстом, остальные — White/Opacity. */
@Composable
fun ChipTabs(tabs: List<Pair<String, String>>, selected: String, feedback: Feedback?, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEach { (id, label) ->
            val active = id == selected
            Box(
                Modifier
                    .clip(PillShape)
                    .background(if (active) Ds.lime else Ds.whiteOpacity)
                    .clickable { feedback?.haptic(); onSelect(id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(label, color = if (active) Ds.black else Ds.whiteSecondary, style = if (active) DsType.Label2B else DsType.Label2R, maxLines = 1)
            }
        }
    }
}

/** Picker: поле Black/Quaternary с градиентной обводкой; выбранный таб — White/Opacity и лаймовый текст. */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, feedback: Feedback?, modifier: Modifier = Modifier, error: Boolean = false, onSelect: (String) -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(FieldShape)
            .background(Ds.blackQuaternary)
            .border(1.dp, if (error) SolidColor(Ds.negative) else Ds.borderBrush, FieldShape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, (id, label) ->
            val active = id == selected
            if (index > 0 && selected.isEmpty()) Box(Modifier.width(0.5.dp).height(28.dp).background(Ds.whiteQuinternary))
            val bg by animateColorAsState(if (active) Ds.whiteOpacity else Color.Transparent, label = "seg")
            Box(
                Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(8.dp)).background(bg)
                    .clickable { feedback?.haptic(); onSelect(id) },
                contentAlignment = Alignment.Center,
            ) { Text(label, color = if (active) Ds.lime else Ds.whiteSecondary, style = DsType.Label2B) }
        }
    }
}

/** Checkbox: 20dp, выбранный — Lime/Accent с галочкой. */
@Composable
fun Checkbox(checked: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.size(20.dp).clip(RoundedCornerShape(6.dp))
            .background(if (checked) Ds.lime else Color.Transparent)
            .then(if (checked) Modifier else Modifier.border(1.5.dp, Ds.whiteQuaternary, RoundedCornerShape(6.dp))),
        contentAlignment = Alignment.Center,
    ) { if (checked) Icon(DsIcons.Check, null, tint = Ds.black, modifier = Modifier.size(14.dp)) }
}

/** SupersetBadge: лаймовая метка «Суперсет» со значком связки. */
@Composable
fun SupersetBadge() {
    Row(
        Modifier.clip(PillShape).background(Ds.lime).padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(DsIcons.Superset, null, tint = Ds.black, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("Суперсет", style = DsType.Label2B, color = Ds.black)
    }
}

/** SupersetLetter: буква упражнения в суперсете (A, B, C). */
@Composable
fun SupersetLetter(letter: String) {
    Box(
        Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(Ds.whiteOpacity).border(1.dp, Ds.limeSecondary, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) { Text(letter, style = DsType.Label2B, color = Ds.lime) }
}

/** ListItem: точка цвета (или иконка на подложке этого цвета), название и подпись; справа шеврон или чекбокс. */
@Composable
fun ChoiceRow(title: String, subtitle: String?, dot: Color?, selected: Boolean = false, showChevron: Boolean = true, checkable: Boolean = false, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(CardShape)
            .background(if (selected) Ds.whiteOpacity else Color.Transparent)
            .then(if (selected) Modifier.border(1.dp, Ds.limeSecondary, CardShape) else Modifier)
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconBadge(icon, dot ?: Ds.whiteTertiary, 36.dp)
            Spacer(Modifier.width(12.dp))
        } else if (dot != null) {
            GlowDot(dot)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = DsType.Body2R)
            if (!subtitle.isNullOrEmpty()) Text(subtitle, style = DsType.Label2R, color = Ds.whiteTertiary)
        }
        when {
            selected || checkable -> Checkbox(selected)
            showChevron -> Icon(DsIcons.ChevronRight, null, tint = Ds.whiteQuaternary, modifier = Modifier.size(18.dp))
        }
    }
}

// ---------------- поля и шторки ----------------

/** Подпись поля (InputLabeled): Label3B, отступ 4. */
@Composable
fun FieldLabel(text: String) {
    Text(text, style = DsType.Label3B, modifier = Modifier.padding(bottom = 4.dp))
}

/** TextField: высота 48, Black/Quaternary, обводка 0.5 — White/Quinternary, в фокусе Lime, при ошибке Negative. */
@Composable
fun BtTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    number: Boolean = false,
    suffix: String? = null,
    leading: ImageVector? = null,
    error: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val border = when {
        error != null -> Ds.negative
        focused -> Ds.lime
        else -> Ds.whiteQuinternary
    }
    Column(modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = DsType.Body2R,
            cursorBrush = SolidColor(Ds.lime),
            interactionSource = interaction,
            keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(FieldShape).background(Ds.blackQuaternary)
                        .border(if (focused || error != null) 1.dp else 0.5.dp, border, FieldShape).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (leading != null) {
                        Icon(leading, null, tint = Ds.whiteTertiary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, style = DsType.Body2R, color = Ds.whiteTertiary, maxLines = 1)
                        inner()
                    }
                    if (suffix != null) Text(suffix, style = DsType.Body2R, color = Ds.whiteTertiary, modifier = Modifier.padding(start = 4.dp))
                }
            },
        )
        if (error != null) Text(error, style = DsType.Label2R, color = Ds.negative, modifier = Modifier.padding(top = 2.dp))
    }
}

/** BottomSheet: Surface/Sheet, радиус 24, ручка 44×4 White/Tertiary, затемнение Black/Tertiary. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BtSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ds.sheet,
        contentColor = Ds.white,
        scrimColor = Ds.blackTertiary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { Box(Modifier.padding(top = 8.dp, bottom = 12.dp).size(44.dp, 4.dp).clip(PillShape).background(Ds.whiteTertiary)) },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp), content = content)
    }
}

@Composable
fun SheetTitle(title: String, subtitle: String? = null) {
    Text(title, style = DsType.Headline1B)
    if (subtitle != null) Text(subtitle, style = DsType.Body2R, color = Ds.whiteTertiary, modifier = Modifier.padding(top = 2.dp))
    Spacer(Modifier.height(16.dp))
}

@Composable
fun FormActions(feedback: Feedback?, confirmLabel: String, enabled: Boolean = true, onCancel: (() -> Unit)?, onConfirm: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onCancel != null) Btn("Отмена", feedback, Modifier.weight(1f), BtnKind.Text, onClick = onCancel)
        Btn(confirmLabel, feedback, Modifier.weight(if (onCancel != null) 1.6f else 1f), enabled = enabled, onClick = onConfirm)
    }
}

/** EmptyState: заголовок Title3B, подсказка Body2R и основное действие. */
@Composable
fun EmptyState(title: String, text: String? = null, image: Painter? = null, icon: ImageVector? = null, action: (@Composable () -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (image != null) Image(image, null, Modifier.size(120.dp).padding(bottom = 12.dp))
        else if (icon != null) Box(Modifier.padding(bottom = 12.dp)) { IconBadge(icon, Ds.whiteTertiary, 48.dp) }
        Text(title, style = DsType.Title3B, color = Ds.whiteSecondary, textAlign = TextAlign.Center)
        if (text != null) Text(text, style = DsType.Body2R, color = Ds.whiteTertiary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        if (action != null) Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) { action() }
    }
}

// ---------------- графика ----------------

/** RestRing: дорожка White/Opacity, прогресс Lime/Accent. */
@Composable
fun Ring(progress: Float, modifier: Modifier = Modifier, color: Color = Ds.lime, track: Color = Ds.whiteOpacity, stroke: Dp = 6.dp) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "ring")
    Canvas(modifier) {
        val w = stroke.toPx()
        val inset = w / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
        drawArc(color, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(w, cap = StrokeCap.Round))
    }
}

/** WheelPicker: строка 44dp; центр — Headline2B на White/Opacity, соседние строки тускнеют. */
@Composable
fun <T> WheelPicker(values: List<T>, initialIndex: Int, label: (T) -> String, feedback: Feedback?, modifier: Modifier = Modifier, onSelected: (T) -> Unit) {
    val itemHeight = 44.dp
    val visible = 5
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex.coerceIn(0, (values.size - 1).coerceAtLeast(0)))
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    // При отступе в 2 строки сверху по центру стоит первый видимый элемент (или следующий, если прокручен больше чем наполовину).
    val selected by remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex + if (listState.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0)
                .coerceIn(0, (values.size - 1).coerceAtLeast(0))
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { selected }.distinctUntilChanged().collect { index ->
            values.getOrNull(index)?.let {
                onSelected(it)
                feedback?.haptic(6)
            }
        }
    }
    Box(modifier.height(itemHeight * visible), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(itemHeight).padding(horizontal = 4.dp).clip(FieldShape).background(Ds.whiteOpacity))
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(vertical = itemHeight * (visible / 2)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(values) { index, value ->
                val distance = kotlin.math.abs(index - selected)
                Box(Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
                    Text(
                        label(value),
                        style = when (distance) {
                            0 -> DsType.Headline2B
                            1 -> DsType.Title3R.copy(color = Ds.whiteQuaternary)
                            else -> DsType.Body3R.copy(color = Ds.whiteQuinternary)
                        }.copy(fontFeatureSettings = "tnum"),
                    )
                }
            }
        }
    }
}
