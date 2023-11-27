package com.example.battlepathsensorgame

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.app.AlertDialog

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val Button1 = findViewById<Button>(R.id.button)
        Button1.setOnClickListener {
            val intent = Intent(this@MainActivity, RunMode::class.java)
            startActivity(intent)
           // finish()
        }
        val Button2 = findViewById<Button>(R.id.button2)
        Button2.setOnClickListener {
            val intent = Intent(this@MainActivity, CompassMode::class.java)
            startActivity(intent)
         //   finish()
        }
        val Button3 = findViewById<Button>(R.id.button3)
        Button3.setOnClickListener {
            val intent = Intent(this@MainActivity, BattleMode::class.java)
            startActivity(intent)
        //    finish()
        }
        val Button4 = findViewById<Button>(R.id.button4)
        Button4.setOnClickListener {
            val intent = Intent(this@MainActivity, HammerMode::class.java)
            startActivity(intent)
         //   finish()
        }

        val Button5 = findViewById<Button>(R.id.button5)
        Button5.setOnClickListener {
            val intent = Intent(this@MainActivity, HTPActivity::class.java)
            startActivity(intent)
        //    finish()
        }
        /*
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        val accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magneticFieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        if (lightSensor == null || accelerometerSensor == null || magneticFieldSensor == null || gyroscopeSensor == null) {
            AlertDialog.Builder(this)
                .setTitle("Sensor Not Available")
                .setMessage("Using this app requires certain sensors that your device does not have.")
                .setPositiveButton("OK") { dialog, which ->
                    finish()
                }
                .show()
        }

         */
    }
}