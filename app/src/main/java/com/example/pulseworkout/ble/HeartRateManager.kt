package com.example.pulseworkout.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import java.util.UUID

data class BleDevice(
    val address: String,
    val name: String,
    val rssi: Int? = null,
    val connectable: Boolean = true,
    val heartRate: Boolean = false,
    val xiaomi: Boolean = false,
    val bonded: Boolean = false,
)

data class WearableState(
    val supported: Boolean? = null,
    val enabled: Boolean? = null,
    val scanning: Boolean = false,
    val status: String = "idle",
    val devices: List<BleDevice> = emptyList(),
    val connected: BleDevice? = null,
    val heartRateSupported: Boolean? = null,
    val bpm: Int? = null,
    val samples: List<Int> = emptyList(),
    val error: String = "",
) {
    /** В списке показываются только устройства, транслирующие стандартный BLE-пульс. */
    val candidates get() = devices.filter { it.connectable && it.heartRate }
    val average get() = if (samples.isEmpty()) null else samples.average().toInt()
    val maximum get() = samples.maxOrNull()
}

/**
 * Нативная замена Capacitor-плагина HeartRateBle: поиск BLE-устройств,
 * подключение по GATT и подписка на Heart Rate Measurement (0x2A37).
 */
@SuppressLint("MissingPermission")
class HeartRateManager(private val context: Context, private val onBpm: (Int) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private var gatt: BluetoothGatt? = null
    private val _state = MutableStateFlow(WearableState())
    val state: StateFlow<WearableState> = _state

    val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasPermissions() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun refreshStatus() {
        _state.update { it.copy(supported = adapter != null, enabled = adapter?.isEnabled == true) }
    }

    fun permissionDenied() {
        _state.update { it.copy(scanning = false, error = "Для поиска браслета нужен доступ к устройствам поблизости") }
    }

    fun startScan() {
        refreshStatus()
        val adapter = adapter
        if (adapter == null) { fail("Bluetooth LE не поддерживается на этом телефоне"); return }
        if (!adapter.isEnabled) { fail("Включите Bluetooth на телефоне"); return }
        if (!hasPermissions()) { permissionDenied(); return }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) { fail("Не удалось запустить Bluetooth-сканер"); return }
        stopScan()
        _state.update { it.copy(devices = emptyList(), error = "", scanning = true) }
        try {
            adapter.bondedDevices?.forEach { device ->
                val name = deviceName(device)
                merge(BleDevice(device.address, name, heartRate = false, xiaomi = looksLikeXiaomi(name), bonded = true))
            }
            scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), scanCallback)
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ stopScan() }, SCAN_DURATION_MS)
        } catch (e: SecurityException) {
            fail("Нет разрешения на поиск Bluetooth-устройств")
        }
    }

    fun stopScan() {
        handler.removeCallbacksAndMessages(null)
        if (_state.value.scanning) {
            try { adapter?.bluetoothLeScanner?.stopScan(scanCallback) } catch (_: SecurityException) {}
            _state.update { it.copy(scanning = false) }
        }
    }

    fun connect(address: String) {
        val adapter = adapter ?: return
        stopScan()
        closeGatt()
        _state.update { it.copy(status = "connecting", error = "", bpm = null, samples = emptyList()) }
        try {
            val device = adapter.getRemoteDevice(address)
            val name = deviceName(device)
            _state.update { it.copy(connected = null) }
            pendingDevice = BleDevice(device.address, name)
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            if (gatt == null) connectionError("Не удалось подключиться к устройству")
        } catch (e: SecurityException) {
            connectionError("Нет разрешения на подключение к Bluetooth-устройству")
        } catch (e: Exception) {
            connectionError("Не удалось подключиться к устройству")
        }
    }

    fun disconnect() {
        closeGatt()
        _state.update { it.copy(status = "disconnected", connected = null, heartRateSupported = null, bpm = null) }
    }

    fun release() {
        stopScan()
        closeGatt()
    }

    private var pendingDevice: BleDevice? = null

    private fun fail(message: String) = _state.update { it.copy(scanning = false, error = message) }

    private fun connectionError(message: String) {
        closeGatt()
        _state.update { it.copy(status = "error", error = message, connected = null, heartRateSupported = null, bpm = null) }
    }

    private fun closeGatt() {
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: SecurityException) {}
        gatt = null
    }

    private fun merge(device: BleDevice) = _state.update { st ->
        val current = st.devices.firstOrNull { it.address == device.address }
        val merged = if (current == null) device else device.copy(
            name = if (device.name == DEFAULT_NAME) current.name else device.name,
            rssi = device.rssi ?: current.rssi,
            heartRate = current.heartRate || device.heartRate,
            xiaomi = current.xiaomi || device.xiaomi,
            bonded = current.bonded || device.bonded,
        )
        val list = st.devices.filter { it.address != device.address } + merged
        st.copy(devices = list.sortedByDescending { it.rssi ?: -999 })
    }

    private fun onScanResult(result: ScanResult) {
        val device = result.device ?: return
        val record = result.scanRecord
        val uuids = record?.serviceUuids.orEmpty().map { it.uuid }
        val name = deviceName(device)
        val xiaomi = XIAOMI_SERVICE in uuids || record?.getServiceData(ParcelUuid(XIAOMI_SERVICE)) != null || looksLikeXiaomi(name)
        val bonded = try { device.bondState == BluetoothDevice.BOND_BONDED } catch (_: SecurityException) { false }
        merge(
            BleDevice(
                address = device.address,
                name = name,
                rssi = result.rssi,
                connectable = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || result.isConnectable,
                heartRate = HEART_RATE_SERVICE in uuids,
                xiaomi = xiaomi,
                bonded = bonded,
            )
        )
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) = this@HeartRateManager.onScanResult(result)
        override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach { this@HeartRateManager.onScanResult(it) }
        override fun onScanFailed(errorCode: Int) = fail("Ошибка поиска: $errorCode")
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _state.update { it.copy(status = "connected", connected = pendingDevice, error = "") }
                try { gatt.discoverServices() } catch (_: SecurityException) { connectionError("Нет разрешения на чтение датчика") }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    closeGatt()
                    _state.update { it.copy(status = "disconnected", connected = null, heartRateSupported = null, bpm = null) }
                } else {
                    connectionError("Устройство разорвало соединение: $status")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val characteristic = gatt.getService(HEART_RATE_SERVICE)?.getCharacteristic(HEART_RATE_MEASUREMENT)
            if (characteristic == null) {
                _state.update { it.copy(heartRateSupported = false, error = "") }
                return
            }
            try {
                gatt.setCharacteristicNotification(characteristic, true)
                characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)?.let { descriptor ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        gatt.writeDescriptor(descriptor)
                    }
                }
                _state.update { it.copy(heartRateSupported = true, error = "") }
            } catch (_: SecurityException) {
                connectionError("Нет разрешения на чтение датчика")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic.uuid == HEART_RATE_MEASUREMENT) handleHeartRate(value)
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
            @Suppress("DEPRECATION")
            if (characteristic.uuid == HEART_RATE_MEASUREMENT) handleHeartRate(characteristic.value)
        }
    }

    private fun handleHeartRate(value: ByteArray?) {
        if (value == null || value.size < 2) return
        val sixteenBit = (value[0].toInt() and 0x01) != 0
        val bpm = if (sixteenBit) {
            if (value.size < 3) return
            (value[1].toInt() and 0xff) or ((value[2].toInt() and 0xff) shl 8)
        } else value[1].toInt() and 0xff
        if (bpm <= 0) return
        _state.update { st -> st.copy(bpm = bpm, samples = (st.samples + bpm).takeLast(3600)) }
        handler.post { onBpm(bpm) }
    }

    private fun deviceName(device: BluetoothDevice): String =
        try { device.name?.trim()?.takeIf { it.isNotEmpty() } } catch (_: SecurityException) { null } ?: DEFAULT_NAME

    private fun looksLikeXiaomi(name: String): Boolean {
        val n = name.lowercase(Locale.ROOT)
        return listOf("xiaomi", "redmi", "mi band", "smart band").any { it in n }
    }

    companion object {
        private const val DEFAULT_NAME = "BLE-устройство"
        private const val SCAN_DURATION_MS = 15_000L
        private val HEART_RATE_SERVICE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        private val HEART_RATE_MEASUREMENT: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        private val CLIENT_CHARACTERISTIC_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val XIAOMI_SERVICE: UUID = UUID.fromString("0000fe95-0000-1000-8000-00805f9b34fb")
    }
}
