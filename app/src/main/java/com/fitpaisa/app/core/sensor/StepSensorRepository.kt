package com.fitpaisa.app.core.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sensorDataStore by preferencesDataStore(name = "fitpaisa_sensor")

/**
 * Tracks physical step count using Android's hardware [Sensor.TYPE_STEP_COUNTER].
 * Persists baseline step count per day to calculate real-time daily steps.
 */
@Singleton
class StepSensorRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private object Keys {
        val BASELINE_STEPS = intPreferencesKey("baseline_steps")
        val BASELINE_DATE = stringPreferencesKey("baseline_date")
        val LAST_TOTAL_STEPS = intPreferencesKey("last_total_steps")
        val BANKED_STEPS = intPreferencesKey("banked_steps")
    }

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepCounterSensor =
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateMutex = Mutex()

    @Volatile
    private var isRegistered = false

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private val _isSensorAvailable = MutableStateFlow(stepCounterSensor != null)
    val isSensorAvailable: StateFlow<Boolean> = _isSensorAvailable.asStateFlow()

    private val _hasPermission = MutableStateFlow(checkPermission())
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    init {
        scope.launch {
            loadInitialSteps()
            startTracking()
        }
    }

    fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun updatePermissionStatus() {
        _hasPermission.value = checkPermission()
        if (_hasPermission.value) startTracking() else unregisterSensor()
    }

    /** Starts (or keeps) the hardware step-counter listener running. */
    fun startTracking() {
        _hasPermission.value = checkPermission()
        if (_hasPermission.value && stepCounterSensor != null) {
            registerSensor()
        }
    }

    @Synchronized
    private fun registerSensor() {
        if (isRegistered) return
        val sensor = stepCounterSensor ?: return
        val manager = sensorManager ?: return
        isRegistered = manager.registerListener(
            this,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    @Synchronized
    fun unregisterSensor() {
        sensorManager?.unregisterListener(this)
        isRegistered = false
    }

    /**
     * Forces a fresh reading from the hardware step counter. Call when the UI
     * becomes visible: the counter keeps counting while the app is closed, but
     * no event fires until the count changes, so the display would otherwise
     * show the last persisted (stale) value. Re-registering makes the system
     * deliver the current total immediately.
     */
    fun refresh() {
        if (!checkPermission() || stepCounterSensor == null) return
        _hasPermission.value = true
        unregisterSensor()
        registerSensor()
    }

    private suspend fun loadInitialSteps() {
        stateMutex.withLock {
            val prefs = context.sensorDataStore.data.first()
            val savedDate = prefs[Keys.BASELINE_DATE]
            val todayStr = LocalDate.now().toString()
            val savedBaseline = prefs[Keys.BASELINE_STEPS] ?: -1
            val lastTotal = prefs[Keys.LAST_TOTAL_STEPS] ?: 0
            val banked = prefs[Keys.BANKED_STEPS] ?: 0

            _todaySteps.value = if (savedDate == todayStr && savedBaseline >= 0) {
                banked + (lastTotal - savedBaseline).coerceAtLeast(0)
            } else {
                0
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val currentHardwareTotal = event.values.getOrNull(0)?.toInt() ?: return

        scope.launch {
            stateMutex.withLock {
                val todayStr = LocalDate.now().toString()
                val prefs = context.sensorDataStore.data.first()
                var baseline = prefs[Keys.BASELINE_STEPS] ?: -1
                var banked = prefs[Keys.BANKED_STEPS] ?: 0
                val savedDate = prefs[Keys.BASELINE_DATE]
                val lastTotal = prefs[Keys.LAST_TOTAL_STEPS] ?: 0
                var mustPersist = false

                when {
                    baseline < 0 -> {
                        // First reading ever: this is the starting point.
                        baseline = currentHardwareTotal
                        banked = 0
                        mustPersist = true
                    }
                    savedDate != todayStr -> {
                        // Midnight passed while the app was in the background.
                        // The last saved hardware total is the closest known
                        // midnight point, so steps after it belong to today.
                        // Do NOT use the current total as the new baseline:
                        // that used to erase every step walked before the
                        // user opened the app for the first time that day.
                        baseline = if (lastTotal > 0 && currentHardwareTotal >= lastTotal) {
                            lastTotal
                        } else {
                            currentHardwareTotal
                        }
                        banked = 0
                        mustPersist = true
                    }
                    currentHardwareTotal < baseline -> {
                        // The phone rebooted today and the hardware counter
                        // restarted. Bank the previous segment, then start a
                        // new segment from the rebooted counter.
                        banked += (lastTotal - baseline).coerceAtLeast(0)
                        baseline = currentHardwareTotal
                        mustPersist = true
                    }
                    currentHardwareTotal - lastTotal >= PERSIST_EVERY_STEPS -> {
                        mustPersist = true
                    }
                }

                if (mustPersist) {
                    context.sensorDataStore.edit {
                        it[Keys.BASELINE_STEPS] = baseline
                        it[Keys.BASELINE_DATE] = todayStr
                        it[Keys.LAST_TOTAL_STEPS] = currentHardwareTotal
                        it[Keys.BANKED_STEPS] = banked
                    }
                }

                _todaySteps.value = banked +
                    (currentHardwareTotal - baseline).coerceAtLeast(0)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private companion object {
        const val PERSIST_EVERY_STEPS = 25
    }
}
