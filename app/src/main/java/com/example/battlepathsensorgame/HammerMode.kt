package com.example.battlepathsensorgame

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.os.Vibrator
import androidx.core.content.ContextCompat.getSystemService
import java.lang.Math.sqrt
import java.security.AccessController.getContext
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class HammerMode : AppCompatActivity(), SensorEventListener {

    private var lastShakeTime: Long = 0
    private var shakeCounter: Int = 0
    private val shakeThreshold = 30f
    private val shakeIntervalMillis = 1000L
    lateinit var task: TextView
    lateinit var score: TextView

    var strong = false
    var isShakeSequenceEnded = false
    var isAll = false
    var isOpened = false

    val shakeDurationMillis: Long = 5000
    var startShakeTime: Long = 0
    var vibrationHandler : Vibrator ?= null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sensorManager = requireNotNull( getSystemService(SensorManager::class.java) )
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL)
        setContentView(R.layout.activity_hammer)

        task = findViewById(R.id.task)
        score = findViewById(R.id.score)

        val blacksmith: ImageView = findViewById(R.id.blacksmith)
        val animation0 = (blacksmith.drawable as? AnimationDrawable)
        animation0?.isOneShot = false
        if (animation0 != null) {
            animation0.start()
        }
        vibrationHandler = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]

        val a = sqrt(ax.pow(2) + ay.pow(2) + az.pow(2))

        if (a > 70f && isShakeSequenceEnded && !isOpened) {
            strong = true
            animate_when_shake(strong, isShakeSequenceEnded)
            isAll = true
            isOpened = true // compass mode is opened oly once :)

            if (isAll) {
                Handler().postDelayed({
                    val intent = Intent(this@HammerMode, CompassMode::class.java)
                    startActivity(intent)
                    finish()
                }, 2000)
            }
        }

        detectFastShakes(a)
    }

    fun detectFastShakes(a: Float) {
        val currentTime = System.currentTimeMillis()
        if (a > shakeThreshold) {
            if (currentTime - lastShakeTime < shakeIntervalMillis) {
                animate_when_shake(strong,isShakeSequenceEnded)
                shakeCounter++
                if (shakeCounter >= 2) {
                    println("Few fast shakes detected!")
                    shakeCounter = 0
                    lastShakeTime = currentTime
                    if (currentTime - startShakeTime >= shakeDurationMillis) {
                        vibrationHandler!!.vibrate(200)
                        task.text = "One last hit! Make a strong one"
                        score.text = ""
                        isShakeSequenceEnded = true
                        animate_when_shake(strong,isShakeSequenceEnded)
                    }
                }
            } else {
                shakeCounter = 1
                lastShakeTime = currentTime
                startShakeTime = currentTime
            }
        }
    }

    fun animate_when_shake(strong: Boolean?,end: Boolean?) {
        val idle_to_work = resources.getDrawable(R.drawable.blacksmith_itw, null)
        val work = resources.getDrawable(R.drawable.blacksmith_work, null)
        val work_to_idle = resources.getDrawable(R.drawable.blacksmith_wti, null)
        val idle = resources.getDrawable(R.drawable.blacksmith_idle, null)

        val animation1 = (idle_to_work as? AnimationDrawable)
        val animation2 = (work as? AnimationDrawable)
        val animation3 = (work_to_idle as? AnimationDrawable)
        val animation4 = (idle as? AnimationDrawable)

        val blacksmith: ImageView = findViewById(R.id.blacksmith)
        if (strong == true) {
            //Idle to Work
            blacksmith.setImageDrawable(animation1)
            if (animation1 != null) {
                animation1.isOneShot = true
                animation1.start()
            }

            //Work
            Handler().postDelayed({
                blacksmith.setImageDrawable(animation2)
                if (animation2 != null) {
                    animation2.isOneShot = true
                    animation2.start()
                }
                vibrationHandler!!.vibrate(200)
                score.text = "Excellent!"
            }, animation1?.numberOfFrames?.times(animation1.getDuration(0))?.toLong() ?: 0)

            //Work to Idle
            Handler().postDelayed(
                {
                    blacksmith.setImageDrawable(animation3)
                    if (animation3 != null) {
                        animation3.isOneShot = true
                        animation3.start()
                    }
                },
                (animation1?.numberOfFrames?.plus(animation2?.numberOfFrames ?: 0)
                    ?: 0) * animation1?.getDuration(0)?.toLong()!!
                    ?: 0
            )
            // Step 4: Idle
            Handler().postDelayed(
                {
                    blacksmith.setImageDrawable(animation4)
                    if (animation4 != null) {
                        animation4.isOneShot = false
                        animation4.start()
                    }
                },
                (animation1?.numberOfFrames?.plus(animation2?.numberOfFrames ?: 0)
                    ?.plus(animation3?.numberOfFrames ?: 0) ?: 0) * animation1?.getDuration(0)
                    ?.toLong()!!
                    ?: 0
            )

        } else if(end==false){
            blacksmith.setImageDrawable(animation1)
            if (animation1 != null) {
                animation1.isOneShot = true
                animation1.start()
            }

            Handler().postDelayed({
                blacksmith.setImageDrawable(animation2)
                if (animation2 != null) {
                    animation2.isOneShot = true
                    animation2.start()
                }
            }, animation1?.numberOfFrames?.times(animation1.getDuration(0))?.toLong() ?: 0)
        }
        else{
            blacksmith.setImageDrawable(animation3)
            if (animation3 != null) {
                animation3.isOneShot = true
                animation3.start()
            }

            Handler().postDelayed({
                blacksmith.setImageDrawable(animation4)
                if (animation4 != null) {
                    animation4.isOneShot = false
                    animation4.start()
                }
            }, animation3?.numberOfFrames?.times(animation3.getDuration(0))?.toLong() ?: 0)
        }
    }
}