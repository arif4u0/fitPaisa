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
    }

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepCounterSensor =
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private val _isSensorAvailable = MutableStateFlow(stepCounterSensor != null)
    val isSensorAvailable: StateFlow<Boolean> = _isSensorAvailable.asStateFlow()

    private val _hasPermission = MutableStateFlow(checkPermission())
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    init {
        scope.launch {
            loadInitialSteps()
            if (hasPermission.value && stepCounterSensor != null) {
                registerSensor()
            }
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
        val granted = checkPermission()
        _hasPermission.value = granted
        if (granted && stepCounterSensor != null) {
            registerSensor()
        }
    }

    private fun registerSensor() {
        stepCounterSensor?.let { sensor ->
            sensorManager?.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun unregisterSensor() {
        sensorManager?.unregisterListener(this)
    }

    private suspend fun loadInitialSteps() {
        val prefs = context.sensorDataStore.data.first()
        val savedDate = prefs[Keys.BASELINE_DATE]
        val todayStr = LocalDate.now().toString()
        val savedBaseline = prefs[Keys.BASELINE_STEPS] ?: -1
        val lastTotal = prefs[Keys.LAST_TOTAL_STEPS] ?: 0

        if (savedDate == todayStr && savedBaseline >= 0) {
            val calculated = (lastTotal - savedBaseline).coerceAtLeast(0)
            _todaySteps.value = calculated
        } else {
            _todaySteps.value = 0
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val currentHardwareTotal = event.values.getOrNull(0)?.toInt() ?: return

        scope.launch {
            val todayStr = LocalDate.now().toString()
            val prefs = context.sensorDataStore.data.first()
            var savedBaseline = prefs[Keys.BASELINE_STEPS] ?: -1
            val savedDate = prefs[Keys.BASELINE_DATE]
            val lastTotal = prefs[Keys.LAST_TOTAL_STEPS] ?: 0

            if (savedDate != todayStr || savedBaseline < 0 || currentHardwareTotal < savedBaseline) {
                // New day or device rebooted (hardware counter reset)
                savedBaseline = currentHardwareTotal
                context.sensorDataStore.edit {
                    it[Keys.BASELINE_STEPS] = savedBaseline
                    it[Keys.BASELINE_DATE] = todayStr
                    it[Keys.LAST_TOTAL_STEPS] = currentHardwareTotal
                }
            } else if (currentHardwareTotal - lastTotal >= PERSIST_EVERY_STEPS) {
                // Throttled persist: the baseline math recovers the exact count
                // from the hardware counter anyway, so every step need not hit storage.
                context.sensorDataStore.edit {
                    it[Keys.LAST_TOTAL_STEPS] = currentHardwareTotal
                }
            }

            val stepsForToday = (currentHardwareTotal - savedBaseline).coerceAtLeast(0)
            _todaySteps.value = stepsForToday
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private companion object {
        const val PERSIST_EVERY_STEPS = 25
    }
}
