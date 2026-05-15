package it.trentosmartmountain.app.data.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Rileva fermata tramite accelerometro (RF6 — conferma auto-pausa insieme alla velocità GPS).
 */
class StationaryDetector(context: Context) : SensorEventListener {

  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

  private val samples = ArrayDeque<Float>(SAMPLE_WINDOW)
  private var _isStationary = true
  val isStationary: Boolean get() = _isStationary

  fun start() {
    accelerometer?.let {
      sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
    }
  }

  fun stop() {
    sensorManager.unregisterListener(this)
    samples.clear()
    _isStationary = true
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
    val magnitude =
      sqrt(
        event.values[0] * event.values[0] +
          event.values[1] * event.values[1] +
          event.values[2] * event.values[2],
      )
    if (samples.size >= SAMPLE_WINDOW) samples.removeFirst()
    samples.addLast(magnitude)
    if (samples.size < SAMPLE_WINDOW) return

    val mean = samples.average().toFloat()
    var variance = 0f
    for (value in samples) {
      val delta = value - mean
      variance += delta * delta
    }
    variance /= samples.size
    _isStationary = variance < VARIANCE_THRESHOLD
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

  companion object {
    private const val SAMPLE_WINDOW = 24
    private const val VARIANCE_THRESHOLD = 0.35f
  }
}
