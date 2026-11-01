package com.example.pulseworkout.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Height
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.StrokeCap
import com.example.pulseworkout.ui.components.CountPill
import com.example.pulseworkout.ui.components.KpiTile
import com.example.pulseworkout.ui.components.RoundIconButton
import com.example.pulseworkout.ui.components.Segmented
import com.example.pulseworkout.ui.theme.BtType
import com.example.pulseworkout.ui.theme.Ds
import com.example.pulseworkout.ui.components.WheelPicker
import com.example.pulseworkout.ui.theme.DsIcons
import com.example.pulseworkout.ui.theme.DsType
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulseworkout.R
import com.example.pulseworkout.data.WeightPoint
import com.example.pulseworkout.logic.Calc
import com.example.pulseworkout.ui.AppViewModel
import com.example.pulseworkout.ui.Sheet
import com.example.pulseworkout.ui.components.Btn
import com.example.pulseworkout.ui.components.BtSheet
import com.example.pulseworkout.ui.components.BtTextField
import com.example.pulseworkout.ui.components.BtnKind
import com.example.pulseworkout.ui.components.ChipTabs
import com.example.pulseworkout.ui.components.FieldLabel
import com.example.pulseworkout.ui.components.FormActions
import com.example.pulseworkout.ui.components.SheetTitle
import com.example.pulseworkout.ui.components.TrendArrow
import com.example.pulseworkout.ui.theme.Bt
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

// ---------- фото профиля: хранится как data:image/jpeg;base64, как в оригинале ----------

private const val PHOTO_PREFIX = "data:image/jpeg;base64,"

@Composable
fun rememberPhotoBitmap(photo: String): ImageBitmap? = remember(photo) {
    if (!photo.startsWith("data:image/")) return@remember null
    runCatching {
        val bytes = Base64.decode(photo.substringAfter(","), Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()
}

/** Квадратная обрезка по центру до 320×320 и JPEG 82% (как resizeProfilePhoto в app.js). */
private fun resizePhoto(context: Context, uri: Uri): String? = runCatching {
    val source = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
    val size = min(source.width, source.height)
    val cropped = Bitmap.createBitmap(source, (source.width - size) / 2, (source.height - size) / 2, size, size)
    val scaled = Bitmap.createScaledBitmap(cropped, 320, 320, true)
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
    PHOTO_PREFIX + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}.getOrNull()

@Composable
private fun rememberPhotoPicker(vm: AppViewModel, onPicked: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val photo = resizePhoto(context, uri)
            if (photo != null) onPicked(photo) else vm.showToast("Не удалось открыть фото")
        }
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

@Composable
private fun Avatar(photo: String, size: androidx.compose.ui.unit.Dp, placeholder: Boolean = false) {
    val bitmap = rememberPhotoBitmap(photo)
    Box(
        Modifier.size(size).border(2.dp, Brush.sweepGradient(listOf(Ds.lime, Ds.limeGradient.copy(alpha = 0.2f), Ds.lime)), CircleShape).padding(4.dp)
            .clip(CircleShape).background(Ds.whiteOpacity),
        contentAlignment = Alignment.Center,
    ) {
        when {
            bitmap != null -> Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            placeholder -> Image(painterResource(R.drawable.photo_placeholder), null, Modifier.size(size * 0.45f))
            else -> Icon(DsIcons.Camera, null, tint = Ds.whiteTertiary, modifier = Modifier.size(size * 0.4f))
        }
    }
}

// ---------- общие поля профиля ----------

private fun parseNumber(value: String) = value.replace(',', '.').toDoubleOrNull()

@Composable
private fun GenderPicker(vm: AppViewModel, gender: String, onChange: (String) -> Unit) {
    Segmented(listOf("male" to "Мужской", "female" to "Женский"), gender, vm.feedback, onSelect = onChange)
}

@Composable
private fun MetricsInputs(age: String, onAge: (String) -> Unit, weight: String, onWeight: (String) -> Unit, height: String, onHeight: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f)) { FieldLabel("Возраст"); BtTextField(age, { onAge(it.filter(Char::isDigit).take(3)) }, placeholder = "—", number = true, suffix = "лет") }
        Column(Modifier.weight(1f)) { FieldLabel("Вес"); BtTextField(weight, { onWeight(it.take(5)) }, placeholder = "—", number = true, suffix = "кг") }
        Column(Modifier.weight(1f)) { FieldLabel("Рост"); BtTextField(height, { onHeight(it.filter(Char::isDigit).take(3)) }, placeholder = "—", number = true, suffix = "см") }
    }
}

private fun fmt(value: Double?) = value?.let { Calc.num(it) }.orEmpty()

private fun validProfile(gender: String, age: Int?, weight: Double?, height: Double?) =
    gender in listOf("male", "female") && (age ?: 0) in 10..120 && (weight ?: 0.0) in 1.0..400.0 && (height ?: 0.0) in 80.0..250.0

// ---------- первый запуск ----------

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    val p = vm.state.profile
    var photo by remember { mutableStateOf(p.photo) }
    var gender by remember { mutableStateOf(p.gender.takeIf { it in listOf("male", "female") }.orEmpty()) }
    var age by remember { mutableStateOf(p.age?.toString().orEmpty()) }
    var weight by remember { mutableStateOf(fmt(p.weight)) }
    var height by remember { mutableStateOf(fmt(p.height)) }
    var name by remember { mutableStateOf(p.name.takeUnless { it.equals("Гость", ignoreCase = true) }.orEmpty()) }
    val pickPhoto = rememberPhotoPicker(vm) { photo = it }
    val complete = gender.isNotEmpty() && age.isNotEmpty() && weight.isNotEmpty() && height.isNotEmpty()

    // Initial Onboarding из макета: логотип и фото-профиль сверху, маскот со свечением, поля, «Продолжить» внизу
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.logo_betr), "BETR", Modifier.height(28.dp))
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.height(28.dp).clip(CircleShape).background(Ds.whiteOpacity).clickable { pickPhoto() }.padding(start = 2.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val bitmap = rememberPhotoBitmap(photo)
                if (bitmap != null) Image(bitmap, null, Modifier.size(24.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                else Icon(DsIcons.Person, null, tint = Ds.lime, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (bitmap != null) "Фото" else "Фото профиля", style = DsType.Label2R)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(180.dp).background(Brush.radialGradient(listOf(Ds.lime.copy(alpha = 0.22f), Color.Transparent)), CircleShape))
                Image(painterResource(R.drawable.onboarding_mascot), null, Modifier.size(140.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("Настройте под себя", style = DsType.Headline2B, textAlign = TextAlign.Center)
            Text("Рост, вес, возраст и пол нужны, чтобы рассчитывать калории, нагрузку и персональную статистику", style = DsType.Body2R, color = Ds.whiteTertiary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth()) {
                FieldLabel("Пол")
                GenderPicker(vm, gender) { gender = it }
                Spacer(Modifier.height(24.dp))
                MetricsInputs(age, { age = it }, weight, { weight = it }, height, { height = it })
                Spacer(Modifier.height(24.dp))
                FieldLabel("Имя")
                BtTextField(name, { name = it.take(24) }, placeholder = "Необязательно")
            }
            Spacer(Modifier.height(20.dp))
        }
        Btn("Продолжить", vm.feedback, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), enabled = complete, height = 58.dp) {
            val a = age.toIntOrNull()
            val w = parseNumber(weight)
            val h = parseNumber(height)
            if (!validProfile(gender, a, w, h)) { vm.showToast("Проверьте возраст, вес и рост"); return@Btn }
            vm.saveProfile(name, gender, a!!, w!!, h!!, photo, fromOnboarding = true)
        }
    }
}

// ---------- профиль ----------

@Composable
fun ProfileSheet(vm: AppViewModel) {
    val p = vm.state.profile
    val bmi = vm.calc.bmi(p.weight, p.height)
    BtSheet(onDismiss = { vm.closeSheet() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(p.photo, 68.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name.ifBlank { "Гость" }, style = DsType.Headline1B)
                Text(
                    listOfNotNull(when (p.gender) { "female" -> "Женщина"; "male" -> "Мужчина"; else -> null }, p.age?.let { Calc.ageLabel(it) }).joinToString(" · ").ifEmpty { "Профиль не заполнен" },
                    style = DsType.Body2R, color = Ds.whiteTertiary,
                )
            }
            Text("Изменить", style = DsType.Label3B, color = Ds.lime, modifier = Modifier.clip(CircleShape).clickable { vm.feedback.haptic(); vm.sheet = Sheet.ProfileEdit }.padding(8.dp))
        }
        Spacer(Modifier.height(18.dp))
        val weightTrend = vm.calc.weightTrendDown(p)
        val bmiTrend = vm.calc.bmiTrendDown(p)
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiTile(DsIcons.Person, "Рост", p.height?.let { Calc.num(it) } ?: "—", Modifier.weight(1f).fillMaxHeight(), unit = "см")
            KpiTile(
                DsIcons.Kettlebell, "Вес", p.weight?.let { Calc.num(it) } ?: "—", Modifier.weight(1f).fillMaxHeight(), unit = "кг",
                caption = weightTrend?.let { if (it) "снизился" else "вырос" },
            )
            KpiTile(
                DsIcons.Weight, "ИМТ", bmi?.let { "%.1f".format(it) } ?: "—", Modifier.weight(1f).fillMaxHeight(),
                caption = vm.calc.bmiCategory(p) + (bmiTrend?.let { if (it) " ↓" else " ↑" } ?: ""),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Динамика веса", style = DsType.Title2B)
            Text("${p.weights.size} ${Calc.plural(p.weights.size, "замер", "замера", "замеров")}", style = DsType.Label2R, color = Ds.whiteTertiary, modifier = Modifier.padding(start = 6.dp))
        }
        Segmented(listOf("week" to "Неделя", "month" to "Месяц", "year" to "Год"), vm.profileChartPeriod, vm.feedback) { vm.profileChartPeriod = it }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ds.whiteOpacity).padding(vertical = 8.dp)) {
            WeightChart(chartPoints(vm.state.profile.weights, vm.profileChartPeriod))
        }
        Btn("Новое взвешивание", vm.feedback, Modifier.fillMaxWidth().padding(top = 16.dp), icon = DsIcons.Plus) { vm.sheet = Sheet.Weighing }
    }
}

private fun chartPoints(weights: List<WeightPoint>, period: String): List<WeightPoint> {
    val days = when (period) { "week" -> 7L; "year" -> 365L; else -> 30L }
    val cutoff = LocalDate.now().minusDays(days - 1).toString()
    return weights.filter { it.weight > 0 && it.date >= cutoff }.sortedBy { "${it.date} ${it.time}" }
}

/** График веса: линия, точки и подписи дат; прокручивается по горизонтали, если точек много. */
@Composable
private fun WeightChart(values: List<WeightPoint>) {
    val density = LocalDensity.current
    val heightDp = 160.dp
    if (values.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(heightDp), contentAlignment = Alignment.Center) {
            Text("Добавь вес в профиле — здесь появится график", color = Color(0x47FFFFFF), fontSize = 12.sp)
        }
        return
    }
    val scroll = rememberScrollState()
    // Прокрутка к последним замерам, как только станет известна ширина графика
    LaunchedEffect(values.size, scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
    val minWidth = 330.dp
    val widthDp = max(minWidth.value, min(8192f, values.size * 76f + 30f)).dp
    Box(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
        Canvas(Modifier.width(widthDp).height(heightDp)) {
            val w = size.width
            val h = size.height
            val minV = values.minOf { it.weight } - 1
            val maxV = values.maxOf { it.weight } + 1
            val left = with(density) { 15.dp.toPx() }
            val baseline = h - with(density) { 42.dp.toPx() }
            val top = with(density) { 30.dp.toPx() }
            val step = if (values.size == 1) 0f else (w - 2 * left) / (values.size - 1)
            fun y(v: Double) = (baseline - ((v - minV) / (maxV - minV).let { if (it == 0.0) 1.0 else it }) * (baseline - top)).toFloat()
            drawLine(Color(0x14FFFFFF), Offset(0f, baseline), Offset(w, baseline), 1f)
            val path = Path()
            values.forEachIndexed { i, p -> val x = left + step * i; if (i == 0) path.moveTo(x, y(p.weight)) else path.lineTo(x, y(p.weight)) }
            // Градиентная заливка под линией
            if (values.size > 1) {
                val area = Path().apply {
                    addPath(path)
                    lineTo(left + step * (values.size - 1), baseline)
                    lineTo(left, baseline)
                    close()
                }
                drawPath(area, Brush.verticalGradient(listOf(Ds.lime.copy(alpha = 0.35f), Color.Transparent), startY = top, endY = baseline))
            }
            drawPath(path, Ds.lime, style = Stroke(width = with(density) { 2.dp.toPx() }, join = StrokeJoin.Round, cap = StrokeCap.Round))
            val stride = max(1, Math.ceil(values.size / max(1.0, Math.floor(w / with(density) { 70.dp.toPx() }.toDouble()))).toInt())
            val datePaint = android.graphics.Paint().apply { isAntiAlias = true; textSize = with(density) { 10.sp.toPx() }; color = Color(0x94FFFFFF).toArgb(); isFakeBoldText = true }
            val timePaint = android.graphics.Paint().apply { isAntiAlias = true; textSize = with(density) { 9.sp.toPx() }; color = Color(0x52FFFFFF).toArgb() }
            val valuePaint = android.graphics.Paint().apply { isAntiAlias = true; textSize = with(density) { 10.sp.toPx() }; color = Bt.text.toArgb(); isFakeBoldText = true; textAlign = android.graphics.Paint.Align.CENTER }
            values.forEachIndexed { i, p ->
                val x = left + step * i
                drawCircle(Ds.sheet, with(density) { 6.dp.toPx() }, Offset(x, y(p.weight)))
                drawCircle(Ds.lime, with(density) { 4.dp.toPx() }, Offset(x, y(p.weight)))
                if (i % stride != 0 && i != values.size - 1) return@forEachIndexed
                val align = when (i) { 0 -> android.graphics.Paint.Align.LEFT; values.size - 1 -> android.graphics.Paint.Align.RIGHT; else -> android.graphics.Paint.Align.CENTER }
                datePaint.textAlign = align
                timePaint.textAlign = align
                drawContext.canvas.nativeCanvas.apply {
                    drawText(Calc.num(p.weight), x, y(p.weight) - with(density) { 9.dp.toPx() }, valuePaint)
                    drawText(Calc.shortDate(p.date), x, h - with(density) { 24.dp.toPx() }, datePaint)
                    drawText(p.time, x, h - with(density) { 9.dp.toPx() }, timePaint)
                }
            }
        }
    }
}

@Composable
fun ProfileEditSheet(vm: AppViewModel) {
    val p = vm.state.profile
    var photo by remember { mutableStateOf(p.photo) }
    var name by remember { mutableStateOf(p.name.takeUnless { it.equals("Гость", ignoreCase = true) }.orEmpty()) }
    var gender by remember { mutableStateOf(p.gender.takeIf { it in listOf("male", "female") }.orEmpty()) }
    var age by remember { mutableStateOf(p.age?.toString().orEmpty()) }
    var weight by remember { mutableStateOf(fmt(p.weight)) }
    var height by remember { mutableStateOf(fmt(p.height)) }
    val pickPhoto = rememberPhotoPicker(vm) { photo = it }

    BtSheet(onDismiss = { vm.sheet = Sheet.Profile }) {
        // Макет «Profile / 3 Edit Profile»: аватар по центру, «Сменить фото», поля профиля
        SheetTitle("Профиль")
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.clickable { pickPhoto() }) { Avatar(photo, 68.dp) }
            Row(Modifier.padding(top = 2.dp)) {
                Btn(if (photo.isNotEmpty()) "Сменить фото" else "Добавить фото", vm.feedback, kind = BtnKind.Text) { pickPhoto() }
                if (photo.isNotEmpty()) Btn("Удалить", vm.feedback, kind = BtnKind.Text) { photo = "" }
            }
        }
        Spacer(Modifier.height(12.dp))
        FieldLabel("Имя")
        BtTextField(name, { name = it.take(24) }, placeholder = "Например: Александр")
        Spacer(Modifier.height(16.dp))
        FieldLabel("Пол")
        GenderPicker(vm, gender) { gender = it }
        Spacer(Modifier.height(16.dp))
        MetricsInputs(age, { age = it }, weight, { weight = it }, height, { height = it })
        FormActions(vm.feedback, "Сохранить", enabled = name.isNotBlank(), onCancel = { vm.sheet = Sheet.Profile }) {
            val a = age.toIntOrNull()
            val w = parseNumber(weight)
            val h = parseNumber(height)
            if (gender !in listOf("male", "female")) { vm.showToast("Выберите пол"); return@FormActions }
            if (!validProfile(gender, a, w, h)) { vm.showToast("Проверьте возраст, вес и рост"); return@FormActions }
            vm.saveProfile(name, gender, a!!, w!!, h!!, photo, fromOnboarding = false)
        }
    }
}

@Composable
fun WeighingSheet(vm: AppViewModel) {
    // Макет «Profile / 4 Weighing»: дата и время, барабан веса с шагом 0,1 кг, предыдущий замер
    val profile = vm.state.profile
    val values = remember { (300..2500).map { it / 10.0 } }
    val start = profile.weight ?: 70.0
    var weight by remember { mutableStateOf(start) }
    val now = remember { java.time.LocalDateTime.now() }
    val previous = profile.weights.filter { it.weight > 0 }.maxByOrNull { "${it.date} ${it.time}" }
    BtSheet(onDismiss = { vm.sheet = Sheet.Profile }) {
        SheetTitle("Новое взвешивание", "${Calc.journalDateLabel(now.toLocalDate().toString())} · %02d:%02d".format(now.hour, now.minute))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Вес", style = DsType.Label2R, color = Ds.whiteTertiary)
            WheelPicker(
                values, values.indices.minByOrNull { kotlin.math.abs(values[it] - start) } ?: 0,
                { "${Calc.num(it)} кг" }, vm.feedback, Modifier.width(220.dp),
            ) { weight = it }
        }
        if (previous != null) {
            Text("Предыдущий замер: ${Calc.num(previous.weight)} кг · ${Calc.journalDateLabel(previous.date)}", style = DsType.Label2R, color = Ds.whiteTertiary, modifier = Modifier.padding(top = 8.dp))
        }
        FormActions(vm.feedback, "Сохранить", onCancel = { vm.sheet = Sheet.Profile }) {
            vm.addWeighing(weight)
        }
    }
}
