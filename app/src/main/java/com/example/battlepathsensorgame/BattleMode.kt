package com.example.battlepathsensorgame

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class BattleMode : AppCompatActivity(), SensorEventListener {

    lateinit var variant: TextView
    lateinit var currentLux: TextView
    lateinit var score: TextView

    var lux : Float = 0.0F
    var hasStartedMainActivity = false

    val regularSet = setOf("darkness", "light")
    var randomValue = getRandomValueFromSet(regularSet)
    val ultimates = setOf("ultimate darkness", "ultimate light")
    val randomUlt = getRandomValueFromSet(ultimates)
    var battleTurnCount = 0
    private var remainingLives = 3

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)
        currentLux = findViewById(R.id.currentLux)
        score = findViewById(R.id.score)
        remainingLives = intent.getIntExtra("remainingLives", 3)

        val lightSensor = requireNotNull( getSystemService(SensorManager::class.java) )
        lightSensor.registerListener(this, lightSensor.getDefaultSensor(Sensor.TYPE_LIGHT),
            SensorManager.SENSOR_DELAY_GAME)

        val knight: ImageView = findViewById(R.id.imageView1)
        val enemy: ImageView = findViewById(R.id.imageView2)

        // Alternative way to access AnimationDrawable
        val animationDrawable1 = (knight.drawable as? AnimationDrawable)
        val animationDrawable2 = (enemy.drawable as? AnimationDrawable)

        // Check if it's not null before using
        animationDrawable1?.isOneShot = false // Set to true if you want it to play only once
        if (animationDrawable1 != null) {
            animationDrawable1.start()
        }
        animationDrawable2?.isOneShot = false // Set to true if you want it to play only once
        if (animationDrawable2 != null) {
            animationDrawable2.start()
        }

        val maxBattleTurns = 5 //number of turns
        battleTurns()
        val battleTimer = object : CountDownTimer((maxBattleTurns * 10 * 1000).toLong(), 10 * 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (score.text != "fail") {
                    randomValue = getRandomValueFromSet(regularSet)
                    drawLightLevel(randomValue)
                    battleTurns()
                    println(battleTurnCount)
                    battleTurnCount++
                }
            }

            override fun onFinish() {
                if (score.text.isBlank() && !hasStartedMainActivity) {
                    if (score.text != "fail" && battleTurnCount == maxBattleTurns) {
                        drawLightLevel(randomUlt)
                        ultTurn()
                    }
                }
            }
        }

        battleTimer.start()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent) {
        lux = event.values[0]
        currentLux.text = "$lux lx"
    }

    fun battleTurns(){
        val timeInSeconds = 10
        val timer = object : CountDownTimer((timeInSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (randomValue == "darkness") {
                    if (lux < 10) {
                        score.text = "great"
                        cancel() // Stop the timer
                        val newResource = resources.getDrawable(R.drawable.player_attack, null)
                        animationChange(newResource)
                    }
                }   else if (lux > 50) {
                    score.text = "great"
                    cancel() // Stop the timer
                    val newResource = resources.getDrawable(R.drawable.player_attack, null)
                    animationChange(newResource)
                }
            }

            override fun onFinish() {
                if (score.text.isBlank() && !hasStartedMainActivity) {
                    hasStartedMainActivity = true
                    score.text = "fail"
                    Handler().postDelayed({
                        val intent = Intent(this@BattleMode, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }, 2000)
                }
            }
        }

        timer.start()
    }

    fun ultTurn(){
        val className: String = if (remainingLives < 3) {
            HammerMode::class.java.name
        } else {
            MainActivity::class.java.name
        }
        val intent = Intent(this@BattleMode, Class.forName(className))
        val timeInSeconds = 10 //time in seconds
        val timer = object : CountDownTimer((timeInSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {

                if (randomUlt == "ultimate darkness"){
                    if(lux < 2){
                        score.text = "great"
                        cancel() // Stop the timer
                        Handler().postDelayed({
                            startActivity(intent)
                            finish()
                        }, 2000)
                    }
                } else {
                    if (lux > 200) {
                        score.text = "great"
                        cancel() // Stop the timer
                        Handler().postDelayed({
                            startActivity(intent)
                            finish()
                        }, 2000)
                    }
                }

            }

            override fun onFinish() {
                if (score.text.isBlank() && !hasStartedMainActivity) {
                    hasStartedMainActivity = true
                    score.text = "fail"
                    Handler().postDelayed({
                        val intent2 = Intent(this@BattleMode, MainActivity::class.java)
                        startActivity(intent2)
                        finish()
                    }, 2000)
                }
            }
        }

        timer.start()

    }

    fun getRandomValueFromSet(valueSet: Set<String>): String? {
        if (valueSet.isEmpty()) {
            return null
        }
        val valueList = valueSet.toList()
        val randomIndex = (0 until valueList.size).random()
        return valueList[randomIndex]
    }

    private fun drawLightLevel(value: String?) {
       variant = findViewById(R.id.variant)
       variant.text = "You need the power of "  + value + " to attack"
    }

    fun animationChange(newResource: Drawable?){
        val newAnimation = (newResource as? AnimationDrawable)
        val knight: ImageView = findViewById(R.id.imageView1)

        if (newAnimation is AnimationDrawable) {
            knight.setImageDrawable(newAnimation)

            newAnimation.isOneShot = true
            newAnimation.start()

            Handler().postDelayed({
                val oldResource = resources.getDrawable(R.drawable.player_idle, null)
                val oldAnimation = (oldResource as? AnimationDrawable)
                knight.setImageDrawable(oldAnimation)
                oldAnimation?.isOneShot = false

                score.text = ""
                variant = findViewById(R.id.variant)
                variant.text = ""

                if (oldAnimation != null) {
                    oldAnimation.start()
                    Handler().postDelayed({
                    },
                        (oldAnimation.numberOfFrames * oldAnimation.getDuration(0)).toLong()
                    )
                }
            },
                (newAnimation.numberOfFrames * newAnimation.getDuration(0)).toLong()
            )
        }
    }

    fun animationChange2(newResourceKnight: Drawable?,newResourceEnemy: Drawable?, oldResourceEnemy: Drawable?){
        val newAnimationKnight = (newResourceKnight as? AnimationDrawable)
        val newAnimationEnemy = (newResourceEnemy as? AnimationDrawable)
        val knight: ImageView = findViewById(R.id.imageView1)
        val enemy: ImageView = findViewById(R.id.imageView2)

        if (newAnimationKnight is AnimationDrawable && newAnimationEnemy is AnimationDrawable) {
            knight.setImageDrawable(newAnimationKnight)
            newAnimationKnight.isOneShot = true
            newAnimationKnight.start()

            enemy.setImageDrawable(newAnimationEnemy)
            newAnimationEnemy.isOneShot = true
            newAnimationEnemy.start()

            Handler().postDelayed({
                val oldResource = resources.getDrawable(R.drawable.player_idle, null)
                val oldAnimation = (oldResource as? AnimationDrawable)
                knight.setImageDrawable(oldAnimation)
                oldAnimation?.isOneShot = false

                val oldAnimationEnemy = (oldResourceEnemy as? AnimationDrawable)
                enemy.setImageDrawable(oldAnimationEnemy)
                oldAnimationEnemy?.isOneShot = false

                score.text = ""
                variant = findViewById(R.id.variant)
                variant.text = ""

                if (oldAnimation != null && oldAnimationEnemy != null) {
                    oldAnimation.start()
                    oldAnimationEnemy.start()
                    Handler().postDelayed({
                    },
                        (oldAnimation.numberOfFrames * oldAnimation.getDuration(0)).toLong()
                    )
                } //run something like: animationChange2(newresK, newresE, null) to avoid this step
            },
                (newAnimationKnight.numberOfFrames * newAnimationKnight.getDuration(0)).toLong()
            )
        }
    }
}