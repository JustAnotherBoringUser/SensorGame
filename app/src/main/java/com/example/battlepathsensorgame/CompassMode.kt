package com.example.battlepathsensorgame

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.round

class CompassMode : AppCompatActivity(), SensorEventListener {
    val randomDirections = setOf("north", "west","south", "east","north-west", "north-east","south-west", "south-east")
    lateinit var randomloc: TextView
    lateinit var result: TextView
    lateinit var compassImageView : ImageView
    var drawnDir = getRandomValueFromSet(randomDirections)
    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    var angle = 0.0
    var direction = ""
    var hasStartedMainActivity = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compass)
        randomloc = findViewById(R.id.randomloc)
        compassImageView = findViewById(R.id.compassImageView)

        randomloc.text = drawnDir
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun getRandomValueFromSet(valueSet: Set<String>): String? {
        if (valueSet.isEmpty()) {
            return null
        }
        val valueList = valueSet.toList()
        val randomIndex = (0 until valueList.size).random()
        return valueList[randomIndex]
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) {
            return
        }

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }
        updateOrientationAngles()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    private fun updateOrientationAngles() {
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)

        val orientation = SensorManager.getOrientation(rotationMatrix, orientationAngles)
        val degrees = (Math.toDegrees(orientation.get(0).toDouble()) + 360.0) % 360.0
        angle = round(degrees * 100) / 100
        direction = getDirection(degrees)

        compassImageView.rotation = angle.toFloat() * -1
        result = findViewById(R.id.result)

        if (!hasStartedMainActivity && direction == drawnDir) {
            result.text = "Great!"
            hasStartedMainActivity = true
           Handler().postDelayed({
                val intent = Intent(this@CompassMode, RunMode::class.java)
                startActivity(intent)
               finish() //prevents going back to this activity after starting next intent
           }, 2000)
        }
    }

    private fun getDirection(angle: Double): String {
        var direction = ""
        if (angle >= 345 || angle <= 15)
            direction = "north"
        if (angle < 330 && angle > 300)
            direction = "north-west"
        if (angle <= 285 && angle > 255)
            direction = "west"
        if (angle <= 240 && angle > 210)
            direction = "south-west"
        if (angle <= 195 && angle > 165)
            direction = "south"
        if (angle <= 150 && angle > 120)
            direction = "south-east"
        if (angle <= 105 && angle > 75)
            direction = "east"
        if (angle <= 60 && angle > 30)
            direction = "north-east"

        return direction
    }
}