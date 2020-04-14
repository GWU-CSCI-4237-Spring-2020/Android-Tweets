package edu.gwu.androidtweets

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.sqrt

class ShakeManager(context: Context) : SensorEventListener {

    private var callback: (() -> Unit)? = null

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val SHAKE_THRESHOLD = 2.5

    private val MIN_TIME_BETWEEN_SHAKES = 2000L

    private var lastShakeTime = 0L

    fun detectShakes(callback: () -> Unit) {
        this.callback = callback

        if(sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).isNotEmpty()) {
            val accelerometer: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
        } else {
            Log.e("ShakeManager", "Device does not have an accelerometer!")
        }
    }

    fun stopDetectingShakes() {
        this.callback = null
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        val currTime = System.currentTimeMillis()
        val timeDifference = currTime - lastShakeTime
        val x: TimeZone

        if (timeDifference > MIN_TIME_BETWEEN_SHAKES) {
            val x = event.values[0].toDouble()
            val y = event.values[1].toDouble()
            val z = event.values[2].toDouble()
            val acceleration = sqrt(x.pow(2.0) + y.pow(2.0) + z.pow(2.0)) - SensorManager.GRAVITY_EARTH

            Log.d("ShakeManager", "[X, Y, Z] = [$x, $y, $z]; Acceleration = $acceleration")
            if (acceleration.absoluteValue > SHAKE_THRESHOLD) {
                callback?.invoke()
                lastShakeTime = currTime
            }
        }
    }
}