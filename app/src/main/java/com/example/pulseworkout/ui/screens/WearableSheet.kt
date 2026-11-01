package com.example.pulseworkout.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pulseworkout.ble.WearableState
import com.example.pulseworkout.ui.AppViewModel
import com.example.pulseworkout.ui.components.Btn
import com.example.pulseworkout.ui.components.BtSheet
import com.example.pulseworkout.ui.components.BtnKind
import com.example.pulseworkout.ui.components.Dot
import com.example.pulseworkout.ui.components.Eyebrow
import com.example.pulseworkout.ui.components.MetricCell
import com.example.pulseworkout.ui.components.Panel
import com.example.pulseworkout.ui.components.SectionTitle
import com.example.pulseworkout.ui.components.SheetTitle
import com.example.pulseworkout.ui.theme.Bt
import com.example.pulseworkout.ui.theme.BtType
import com.example.pulseworkout.ui.theme.Ds
import com.example.pulseworkout.ui.theme.DsIcons
import com.example.pulseworkout.ui.theme.DsType
import com.example.pulseworkout.ui.components.EmptyState
import com.example.pulseworkout.ui.components.IconBadge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Search

private fun statusText(w: WearableState): String = when {
    w.error.isNotEmpty() -> w.error
    w.connected != null && w.heartRateSupported == false -> "Подключено, но прямой пульс недоступен"
    w.connected != null && w.heartRateSupported == true -> "Пульс передаётся в реальном времени"
    w.status == "connecting" -> "Подключение…"
    w.scanning -> "Ищем устройства рядом…"
    w.enabled == false -> "Включите Bluetooth на телефоне"
    else -> "Браслет не подключён"
}

@Composable
fun WearableSheet(vm: AppViewModel) {
    val w by vm.wearable.state.collectAsState()
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) vm.wearable.startScan() else vm.wearable.permissionDenied()
    }
    LaunchedEffect(Unit) { vm.wearable.refreshStatus() }
    val scan = {
        if (vm.wearable.hasPermissions()) vm.wearable.startScan() else permissions.launch(vm.wearable.requiredPermissions)
    }

    BtSheet(onDismiss = { vm.closeSheet() }) {
        Eyebrow("Тестовое подключение")
        SheetTitle("Браслет")
        // WearableCard из макета «08 Wearable»
        Panel(borderColor = if (w.bpm != null) Ds.lime else androidx.compose.ui.graphics.Color.Transparent) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(DsIcons.Radiowaves, null, tint = Ds.whiteTertiary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Eyebrow(w.connected?.name ?: "Пульс")
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(DsIcons.Heart, Ds.negative, 40.dp)
                Spacer(Modifier.width(12.dp))
                Text("${w.bpm ?: "—"}", style = DsType.Display1B)
                Text(" уд/мин", style = DsType.Body2R, color = Ds.whiteTertiary, modifier = Modifier.padding(top = 12.dp))
            }
            Text(statusText(w), color = if (w.error.isNotEmpty()) Ds.negative else Ds.lime, style = DsType.Label2R)
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCell("${w.average ?: "—"}", "Средний", Modifier.weight(1f))
                MetricCell("${w.maximum ?: "—"}", "Максимальный", Modifier.weight(1f))
            }
        }
        Panel(Modifier.padding(top = 10.dp)) {
            Text("Включите трансляцию на браслете", style = DsType.Title1B)
            Text("Настройки → Настройки тренировки → Трансляция пульса", style = DsType.Label2R, color = Ds.whiteTertiary)
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Btn(if (w.scanning) "Идёт поиск…" else "Найти браслет", vm.feedback, Modifier.weight(1f), enabled = !w.scanning && w.status != "connecting", icon = DsIcons.Search) { scan() }
            if (w.connected != null) Btn("Отключить", vm.feedback, Modifier.weight(1f), BtnKind.Secondary) { vm.wearable.disconnect() }
        }
        SectionTitle("Датчики пульса", trailing = "${w.candidates.size}")
        Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
            if (w.candidates.isEmpty()) {
                EmptyState(
                    if (w.scanning) "Ждём трансляцию пульса…" else "Датчик пульса не найден",
                    "Включите трансляцию пульса на браслете\nи повторите поиск",
                    icon = DsIcons.Radiowaves,
                )
            }
            w.candidates.forEach { device ->
                val connected = w.connected?.address == device.address
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (connected) Ds.whiteOpacity else Ds.blackQuaternary)
                        .border(1.dp, if (connected) Ds.limeSecondary else Ds.whiteQuinternary, RoundedCornerShape(12.dp))
                        .clickable(enabled = w.status != "connecting") { vm.feedback.haptic(); vm.wearable.connect(device.address) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Dot(if (connected) Bt.lime else Bt.muted2)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(device.name.ifBlank { "Фитнес-браслет" }, style = DsType.Body2B)
                        Text("${device.address} · ${device.rssi?.let { "$it dBm" } ?: "сигнал неизвестен"} · трансляция пульса", style = DsType.Label1R, color = Ds.whiteTertiary)
                    }
                    Text(if (connected) "Подключён" else "Подключить", style = DsType.Label2B, color = Ds.lime)
                }
            }
        }
        Text("В этом списке отображаются только устройства, которые передают стандартный Bluetooth-сигнал пульса.", style = DsType.Label1R, color = Ds.whiteQuaternary, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(4.dp))
    }
}
