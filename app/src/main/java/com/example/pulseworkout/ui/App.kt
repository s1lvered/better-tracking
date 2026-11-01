package com.example.pulseworkout.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsGymnastics
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulseworkout.logic.Calc
import com.example.pulseworkout.ui.components.Btn
import com.example.pulseworkout.ui.components.BtnKind
import com.example.pulseworkout.ui.components.IconBadge
import com.example.pulseworkout.ui.screens.ActiveSessionScreen
import com.example.pulseworkout.ui.screens.CardioFinishSheet
import com.example.pulseworkout.ui.screens.CardioManualSheet
import com.example.pulseworkout.ui.screens.ExerciseChooserSheet
import com.example.pulseworkout.ui.screens.ExerciseEditSheet
import com.example.pulseworkout.ui.screens.JournalScreen
import com.example.pulseworkout.ui.screens.LibraryScreen
import com.example.pulseworkout.ui.screens.OnboardingScreen
import com.example.pulseworkout.ui.screens.ProfileEditSheet
import com.example.pulseworkout.ui.screens.ProfileSheet
import com.example.pulseworkout.ui.screens.SetEntrySheet
import com.example.pulseworkout.ui.screens.SplitEditSheet
import com.example.pulseworkout.ui.screens.SplitPickerSheet
import com.example.pulseworkout.ui.screens.SplitsScreen
import com.example.pulseworkout.ui.screens.StartWorkoutSheet
import com.example.pulseworkout.ui.screens.WearableSheet
import com.example.pulseworkout.ui.screens.WeighingSheet
import com.example.pulseworkout.ui.screens.WorkoutStatsSheet
import com.example.pulseworkout.ui.screens.rememberPhotoBitmap
import com.example.pulseworkout.BuildConfig
import com.example.pulseworkout.R
import com.example.pulseworkout.update.UpdateState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.pulseworkout.ui.components.GlowDot
import com.example.pulseworkout.ui.components.PillShape
import com.example.pulseworkout.ui.theme.Bt
import com.example.pulseworkout.ui.theme.Ds
import com.example.pulseworkout.ui.theme.DsIcons
import com.example.pulseworkout.ui.theme.DsType
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay

@Composable
fun BetterTrackingApp(vm: AppViewModel) {
    @Suppress("UNUSED_VARIABLE") val revision = vm.revision // подписка на изменения модели

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Ds.screenTop, Ds.screenBottom)))
            .drawBehind {
                // Фон экрана из макетов: тёмный градиент с лаймовым радиальным отсветом
                drawRect(Brush.radialGradient(listOf(Ds.screenGlow.copy(alpha = 0.9f), Color.Transparent), center = Offset(size.width * 0.5f, size.height * 0.35f), radius = size.width * 1.2f))
            }
    ) {
        if (!vm.calc.isProfileComplete() && !vm.state.profileOnboardingSkipped) {
            OnboardingScreen(vm)
        } else {
            MainScaffold(vm)
        }
        ToastHost(vm)
    }

    vm.confirm?.let { request ->
        // Dialog из дизайн-системы: Surface/Sheet, радиус 24, Plain + Primary
        AlertDialog(
            onDismissRequest = { vm.confirm = null },
            containerColor = Ds.sheet,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.border(1.dp, Ds.whiteOpacity, RoundedCornerShape(24.dp)),
            title = { Text(request.title, style = DsType.Title3B) },
            text = { Text(request.message, style = DsType.Body2R, color = Ds.whiteTertiary) },
            confirmButton = {
                Btn(request.confirmLabel, vm.feedback, height = 44.dp) {
                    vm.confirm = null
                    request.onConfirm()
                }
            },
            dismissButton = { Btn("Отмена", vm.feedback, kind = BtnKind.Text, height = 44.dp) { vm.confirm = null } },
        )
    }

    vm.update?.let { UpdateDialog(vm, it) }

    when (val sheet = vm.sheet) {
        null -> Unit
        Sheet.StartWorkout -> StartWorkoutSheet(vm)
        is Sheet.ExerciseChooser -> ExerciseChooserSheet(vm, sheet)
        is Sheet.SetEntry -> SetEntrySheet(vm, sheet)
        is Sheet.CardioFinish -> CardioFinishSheet(vm, sheet)
        is Sheet.CardioManual -> CardioManualSheet(vm, sheet)
        is Sheet.WorkoutStats -> WorkoutStatsSheet(vm, sheet)
        Sheet.Profile -> ProfileSheet(vm)
        Sheet.ProfileEdit -> ProfileEditSheet(vm)
        Sheet.Weighing -> WeighingSheet(vm)
        is Sheet.ExerciseEdit -> ExerciseEditSheet(vm, sheet.exerciseId)
        is Sheet.SplitEdit -> SplitEditSheet(vm, sheet.splitId)
        is Sheet.SplitPicker -> SplitPickerSheet(vm, sheet.splitId)
        Sheet.Wearable -> WearableSheet(vm)
    }
}

@Composable
private fun MainScaffold(vm: AppViewModel) {
    BackHandler(enabled = vm.tab != "journal") { vm.tab = "journal" }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Header(vm)
        val screen = if (vm.tab == "log" && vm.session == null) "journal" else vm.tab
        AnimatedContent(screen, Modifier.weight(1f), transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "tab") { tab ->
            when (tab) {
                "exercises" -> LibraryScreen(vm)
                "splits" -> SplitsScreen(vm)
                "log" -> ActiveSessionScreen(vm)
                else -> JournalScreen(vm)
            }
        }
    }
    BottomNav(vm)
}

@Composable
private fun Header(vm: AppViewModel) {
    // Header: Logo Horisontal 94×20 и ProfileTab (капсула White/Opacity, аватар 24, Label2R)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(R.drawable.logo_betr), "BETR", Modifier.height(20.dp))
        Spacer(Modifier.weight(1f))
        ProfileTab(vm.state.profile.name.ifBlank { "Профиль" }, vm.state.profile.photo) { vm.feedback.haptic(); vm.sheet = Sheet.Profile }
    }
}

@Composable
private fun ProfileTab(name: String, photoData: String, onClick: () -> Unit) {
    Row(
        Modifier.height(28.dp).clip(PillShape).background(Ds.whiteOpacity).clickable(onClick = onClick).padding(start = 2.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val photo = rememberPhotoBitmap(photoData)
        Box(Modifier.size(24.dp).clip(CircleShape), contentAlignment = Alignment.Center) {
            if (photo != null) Image(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Icon(DsIcons.Person, null, tint = Ds.lime, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text(name, style = DsType.Label2R, maxLines = 1)
    }
}

@Composable
private fun BottomNav(vm: AppViewModel) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        // Затемнение под навигацией, чтобы контент мягко уходил под неё
        Box(Modifier.fillMaxWidth().height(110.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Ds.screenBottom.copy(alpha = 0.95f)))))
        // Tabbar: контейнер Black/Secondary (капсула, отступ 4), справа FAB 56 с гантелей
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(PillShape)
                    .background(Ds.blackSecondary)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NavItem(vm, "journal", "Журнал", DsIcons.Journal, Modifier.weight(1f))
                NavItem(vm, "exercises", "Упражнения", DsIcons.Exercise, Modifier.weight(1f))
                NavItem(vm, "splits", "Сплиты", DsIcons.Splits, Modifier.weight(1f))
            }
            if (vm.session != null || vm.state.workouts.isNotEmpty()) {
                Spacer(Modifier.width(10.dp))
                WorkoutButton(vm)
            }
        }
    }
}

/** Кнопка тренировки: в покое — «начать», во время тренировки — пульсирующая с таймером. */
@Composable
private fun WorkoutButton(vm: AppViewModel) {
    val live = vm.session?.editingWorkoutId == null && vm.session != null
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(1f, if (live) 1.06f else 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "scale")
    Box(
        Modifier
            .scale(pulse)
            .size(56.dp)
            .shadow(if (live) 18.dp else 0.dp, CircleShape, ambientColor = Ds.lime, spotColor = Ds.lime)
            .clip(CircleShape)
            .background(Ds.primaryBrush)
            .clickable { vm.feedback.haptic(); vm.onWorkoutButton() },
        contentAlignment = Alignment.Center,
    ) {
        if (live) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(DsIcons.Dumbbell, null, tint = Ds.black, modifier = Modifier.size(18.dp))
                Text(Calc.clock(vm.sessionElapsedSeconds()), style = DsType.Footnote.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), color = Ds.black)
            }
        } else {
            Icon(DsIcons.Dumbbell, "Начать тренировку", tint = Ds.black, modifier = Modifier.size(24.dp))
        }
    }
}

/** Tabbar Button: активная — радиальное лаймовое свечение, иконка и подпись Lime/Accent. */
@Composable
private fun NavItem(vm: AppViewModel, id: String, label: String, icon: ImageVector, modifier: Modifier) {
    // Как в макетах: экран тренировки открыт из журнала, поэтому «Журнал» остаётся активным.
    val active = vm.tab == id || (id == "journal" && vm.tab == "log")
    Column(
        modifier
            .fillMaxHeight()
            .clip(PillShape)
            .background(if (active) Brush.radialGradient(listOf(Ds.lime.copy(alpha = 0f), Ds.lime.copy(alpha = 0.22f))) else SolidColor(Color.Transparent))
            .clickable { vm.feedback.haptic(); vm.tab = id }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (active) Ds.lime else Ds.whiteTertiary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, style = DsType.Label2R, color = if (active) Ds.lime else Ds.whiteTertiary, maxLines = 1)
    }
}

@Composable
private fun ToastHost(vm: AppViewModel) {
    var visible by remember { mutableStateOf(false) }
    val toast = vm.toast
    LaunchedEffect(toast) {
        if (toast != null) {
            visible = true
            delay(2200)
            visible = false
        }
    }
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 100.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
            // Toast: капсула Black/Secondary с индикатором
            Row(
                Modifier
                    .padding(horizontal = 24.dp)
                    .clip(PillShape)
                    .background(Color(0xF2212121))
                    .border(1.dp, Ds.whiteOpacity, PillShape)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlowDot()
                Spacer(Modifier.width(8.dp))
                Text(toast?.first.orEmpty(), style = DsType.Body2R)
            }
        }
    }
}

/** Попап обновления с GitHub: предложение, загрузка с прогрессом, установка, ошибка. */
@Composable
private fun UpdateDialog(vm: AppViewModel, state: UpdateState) {
    val release = when (state) {
        is UpdateState.Available -> state.release
        is UpdateState.Downloading -> state.release
        is UpdateState.Ready -> state.release
        is UpdateState.Failed -> state.release
    }
    val sizeMb = release.apk?.size?.takeIf { it > 0 }?.let { "%.1f МБ".format(it / 1_048_576.0) }
    AlertDialog(
        onDismissRequest = { if (state !is UpdateState.Downloading) vm.dismissUpdate() },
        containerColor = Ds.sheet,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, Ds.whiteOpacity, RoundedCornerShape(24.dp)),
        title = {
            Text(
                when (state) {
                    is UpdateState.Available -> "Доступна версия ${release.version}"
                    is UpdateState.Downloading -> "Загрузка обновления"
                    is UpdateState.Ready -> "Обновление загружено"
                    is UpdateState.Failed -> "Обновление не загружено"
                },
                style = DsType.Title3B,
            )
        },
        text = {
            // Одинаковая ширина попапа во всех состояниях
            Column(Modifier.width(300.dp)) {
                when (state) {
                    is UpdateState.Available -> {
                        Text(
                            "У вас версия ${BuildConfig.VERSION_NAME}." + (sizeMb?.let { " Размер обновления — $it." } ?: ""),
                            style = DsType.Body2R, color = Ds.whiteTertiary,
                        )
                        val notes = release.body?.trim().orEmpty()
                        if (notes.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Что нового", style = DsType.Label3B)
                            Text(
                                notes, style = DsType.Body2R, color = Ds.whiteSecondary,
                                modifier = Modifier.padding(top = 4.dp).heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                            )
                        }
                    }
                    is UpdateState.Downloading -> {
                        val p = state.progress
                        Box(Modifier.fillMaxWidth().height(6.dp).clip(PillShape).background(Ds.whiteOpacity)) {
                            Box(Modifier.fillMaxWidth(p ?: 0.15f).height(6.dp).clip(PillShape).background(Ds.primaryBrush))
                        }
                        Text(p?.let { "${(it * 100).toInt()}%" + (sizeMb?.let { s -> " из $s" } ?: "") } ?: "Скачиваем…", style = DsType.Label2R, color = Ds.whiteTertiary, modifier = Modifier.padding(top = 8.dp))
                    }
                    is UpdateState.Ready -> Text("Нажмите «Установить» — Android попросит подтвердить обновление. Данные приложения сохранятся.", style = DsType.Body2R, color = Ds.whiteTertiary)
                    is UpdateState.Failed -> Text(state.message, style = DsType.Body2R, color = Ds.whiteTertiary)
                }
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.Available -> Btn(if (release.apk != null) "Скачать" else "Открыть GitHub", vm.feedback, height = 44.dp) { vm.downloadUpdate() }
                is UpdateState.Downloading -> Unit
                is UpdateState.Ready -> Btn("Установить", vm.feedback, height = 44.dp) { vm.installUpdate() }
                is UpdateState.Failed -> Btn("Повторить", vm.feedback, height = 44.dp) { vm.downloadUpdate() }
            }
        },
        dismissButton = {
            Btn(if (state is UpdateState.Downloading) "Отмена" else "Позже", vm.feedback, kind = BtnKind.Text, height = 44.dp) { vm.dismissUpdate() }
        },
    )
}

/** Отступ снизу у прокручиваемых экранов, чтобы контент не прятался под навигацией. */
val BottomNavSpace = 120.dp

@Composable
fun BottomSpacer() = Spacer(Modifier.height(BottomNavSpace))
