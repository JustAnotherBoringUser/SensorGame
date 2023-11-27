package com.example.battlepathsensorgame

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.core.content.res.ResourcesCompat

class RunMode : AppCompatActivity(), SensorEventListener {

    private var mSensorManager: SensorManager? = null
    private var mGyroscope: Sensor? = null
    private var thread: DrawThread? = null
    var ground: GroundView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mGyroscope = mSensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_IMMERSIVE
                    )
        }

        ground = GroundView(this)
        setContentView(ground)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            ground!!.updateMe(event.values[1])
        }
    }

    override fun onResume() {
        super.onResume()
        thread?.setRunning(true)
        mSensorManager?.registerListener(
            this,
            mGyroscope,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onPause() {
        super.onPause()
        thread?.setRunning(false)
        //mSensorManager!!.unregisterListener(this)
        mSensorManager?.unregisterListener(this)
    }

    class DrawThread (surfaceHolder: SurfaceHolder , panel : GroundView) : Thread() {
        private var surfaceHolder :SurfaceHolder ?= null
        private var panel : GroundView ?= null
        private var run = false

        init {
            this.surfaceHolder = surfaceHolder
            this.panel = panel
        }

        fun setRunning(run : Boolean){
            this.run = run
        }

        override fun run() {
            var c: Canvas ?= null
            while (run){
                c = null
                try {
                    if(surfaceHolder != null && panel != null){
                        synchronized(surfaceHolder!!) {
                            c = surfaceHolder!!.lockCanvas(null)
                            if (c != null) {
                                panel!!.draw(c)
                            }
                        }
                    }
                }finally {
                    if (c!= null){
                        surfaceHolder!!.unlockCanvasAndPost(c)
                    }
                }
            }
        }

    }

}


class GroundView(context: Context?) : SurfaceView(context), SurfaceHolder.Callback{
    private var speed = 5
    private var time = 0
    private var playerPosition = 0
    private val obstacles = ArrayList<HashMap<String, Any>>()
    private var lives = 3
    private var points = 0
    private val animationFrames = ArrayList<Drawable>()
    private val damagedAnimationFrames = ArrayList<Drawable>()
    private var isDamaged = false

    private var isTransitionInitiated = false
    private val handler = Handler(Looper.getMainLooper())

    private var frameIndex = 0
    private var frameDuration = 100 // duration of each frame in milliseconds
    private var lastFrameChangeTime = 0L
    private val deathAnimationFrames = ArrayList<Drawable>()
    private var isDeathAnimationActive = false
    private var deathFrameIndex = 0
    private var deathLastFrameChangeTime = 0L
    private var deathFrameDuration = 100
    val typeface = ResourcesCompat.getFont(context!!, R.font.bitfantasy)

    private var enemyPosition = 0
    private val enemyAnimationFrames = ArrayList<Drawable>()
    private var isEnemyActive = false
    private var isEnemyMiddle = false
    private var enemyFrameIndex = 0
    private var enemyLastFrameChangeTime = 0L
    private var enemyFrameDuration = 100
    private var enemySlideSpeed = 10
    private var enemyAppearCountdown = 10

    // last position
    var lastPos : Float = 0.toFloat()
    var picHeight: Int = 0
    var picWidth : Int = 0
    var viewWidth = 0
    var viewHeight = 0

    var noBorder = false
    var thread : RunMode.DrawThread?= null
    var xAxis: Float = 10.toFloat()
    var backgroundBitmap: Bitmap? = null

    init {
        holder.addCallback(this)
        //create a thread
        thread = RunMode.DrawThread(holder, this)
        val display: Display = (getContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val size:Point = Point()
        display.getSize(size)

        picWidth =  viewWidth / 5
        picHeight =  picWidth + 10

        for (i in 1..8) {
            val frameId = resources.getIdentifier("frame$i", "drawable", context!!.packageName)
            val frame = context.getDrawable(frameId)
            animationFrames.add(frame!!)
        }
        for (i in 1..3) {
            val frameId = resources.getIdentifier("damaged$i", "drawable", context!!.packageName)
            val frame = context.getDrawable(frameId)
            damagedAnimationFrames.add(frame!!)
        }
        for (i in 1..5) {
            val frameId = resources.getIdentifier("death$i", "drawable", context!!.packageName)
            val frame = context.getDrawable(frameId)
            deathAnimationFrames.add(frame!!)
        }

        for (i in 1..5) {
            val frameId = resources.getIdentifier("enemy$i", "drawable", context!!.packageName)
            val frame = context.getDrawable(frameId)
            enemyAnimationFrames.add(frame!!)
        }

        backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.road)
    }

    override fun surfaceChanged(p0: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
        thread!!.setRunning(true)
        thread!!.start()
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        viewWidth = this.measuredWidth
        viewHeight = this.measuredHeight
        val laneWidth = viewWidth - viewWidth / 5

        if (canvas != null) {
            backgroundBitmap?.let {
                val scaleWidth = viewWidth.toFloat() / it.width.toFloat()
                val scaleHeight = viewHeight.toFloat() / it.height.toFloat()

                val scaledWidth = (it.width * scaleWidth).toInt()
                val scaledHeight = (it.height * scaleHeight).toInt()

                val offsetX = (viewWidth - scaledWidth) / 2F
                val offsetY = (viewHeight - scaledHeight) / 2F

                val matrix = Matrix()
                matrix.postScale(scaleWidth, scaleHeight)

                val scaledBitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
                canvas.drawBitmap(scaledBitmap, offsetX, offsetY, null)
            }
        }

        val paint = Paint()
        paint.color = Color.WHITE
        paint.textSize = 50f
        paint.typeface = typeface
        canvas?.drawText("Lives: $lives", 50f, 100f, paint)

        if (time % 700 < 10 + speed) {
                if (points >= enemyAppearCountdown && !isEnemyActive) {
                    isEnemyActive = true
                }

                if (!isEnemyActive) {
                    val map = HashMap<String, Any>()
                    map["lane"] = (0..3).random()
                    map["startTime"] = time
                    obstacles.add(map)
                }
        }
        time = time + 10 + speed

        if (canvas != null) {
            val obstacleWidth = viewWidth / 5
            val obstacleHeight = obstacleWidth + 20

            if (!isDeathAnimationActive) {

                val iterator = obstacles.iterator()
                while (iterator.hasNext()) {
                    val singleObstacle = iterator.next()
                    try {
                        val lane = singleObstacle["lane"] as Int
                        val singleX = lane * laneWidth / 4 + viewWidth / 10
                        val singleY = time - singleObstacle["startTime"] as Int

                        val obstacleDrawable = resources.getDrawable(R.drawable.blur, null)

                        obstacleDrawable.setBounds(
                            singleX,
                            singleY - picHeight,
                            singleX +  picWidth,
                            singleY
                        )
                        obstacleDrawable.draw(canvas)

                        val obstacleDrawable2 = resources.getDrawable(R.drawable.blur, null)
                        obstacleDrawable2.setBounds(
                            singleX,
                            singleY - obstacleHeight,
                            singleX + obstacleWidth,
                            singleY
                        )
                        obstacleDrawable2.draw(canvas)

                        if (lane == playerPosition && singleY < viewHeight && singleY > viewHeight - picHeight) {
                            singleObstacle["collisionDetected"] = true // the collision
                            isDamaged = true
                            postDelayed({
                                isDamaged = false
                                singleObstacle["collisionDetected"] = false
                            }, 500)
                            lives--
                        }

                        if (singleY > viewHeight + picHeight) {
                            iterator.remove()
                            points++
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            val obstacleHeightInt = obstacleHeight.toInt()
            val obstacleWidthInt = obstacleWidth.toInt()

            val frames = when {
               // isDeathAnimationActive -> deathAnimationFrames
                isDamaged -> damagedAnimationFrames
                else -> animationFrames
            }

            if (lives <= 0) {
                if (!isDeathAnimationActive) {
                    isDeathAnimationActive = true
                    deathFrameIndex = 0
                    deathLastFrameChangeTime = System.currentTimeMillis()
                }
            } else {
                isDeathAnimationActive = false
            }

            if (isDeathAnimationActive) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - deathLastFrameChangeTime > deathFrameDuration) {
                    deathFrameIndex++
                    if (deathFrameIndex < deathAnimationFrames.size) {
                        val currentDeathFrame = deathAnimationFrames[deathFrameIndex]
                        currentDeathFrame.setBounds(
                            (xAxis + viewWidth / 15 + 25).toInt(),
                            viewHeight - 2 - obstacleHeightInt,
                            (xAxis + viewWidth / 15 + obstacleWidthInt - 25).toInt(),
                            viewHeight - 10
                        )
                        currentDeathFrame.draw(canvas)
                        deathLastFrameChangeTime = currentTime
                    } else {
                        // End of death animation frames
                        val lastFrame = context.getDrawable(R.drawable.death5)
                        lastFrame?.let {
                            it.setBounds(
                                (xAxis + viewWidth / 15 + 25).toInt(),
                                viewHeight - 2 - obstacleHeightInt,
                                (xAxis + viewWidth / 15 + obstacleWidthInt - 25).toInt(),
                                viewHeight - 10
                            )
                            it.draw(canvas)
                        }

                        val paint = Paint()
                        paint.color = Color.RED
                        paint.textSize = 100f
                        paint.typeface = ResourcesCompat.getFont(context, R.font.bitfantasy)
                        val text = "Game Over"
                        val textWidth = paint.measureText(text)
                        val x = (viewWidth - textWidth) / 2
                        val y = viewHeight / 2
                        canvas.drawText(text, x.toFloat(), y.toFloat(), paint)

                        postDelayed({
                            if (!isTransitionInitiated) {
                                isTransitionInitiated = true
                                handler.post {
                                    val intent = Intent(context, MainActivity::class.java)
                                    context.startActivity(intent)
                                    (context as Activity).finish()
                                }
                            }
                        }, 3000)
                    }
                }
            }  else {
                if (isEnemyActive) {
                    drawEnemy(canvas)
                }
                if(isEnemyMiddle){
                    val playerStopFrame = context.getDrawable(R.drawable.frame1)
                    playerStopFrame?.let {
                        it.setBounds(
                            (xAxis + viewWidth / 15 + 25).toInt(),
                            viewHeight - 2 - obstacleHeightInt,
                            (xAxis + viewWidth / 15 + obstacleWidthInt - 25).toInt(),
                            viewHeight - 10
                        )
                        it.draw(canvas)
                    }
                } else{
                   // if (frameIndex >= 0 && frameIndex < frames.size) {
                  //      val currentFrame = frames[frameIndex]
                    val framesSize = frames.size
                    if (framesSize > 0) {
                        val currentFrameIndex = frameIndex % framesSize
                        val currentFrame = frames[currentFrameIndex]
                        currentFrame.setBounds(
                            (xAxis + viewWidth / 15 + 25).toInt(),
                            viewHeight - 2 - obstacleHeightInt,
                            (xAxis + viewWidth / 15 + obstacleWidthInt - 25).toInt(),
                            viewHeight - 10
                        )
                        currentFrame.draw(canvas)
                    }

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastFrameChangeTime > frameDuration) {
                        frameIndex = (frameIndex + 1) % (if (isDamaged) 3 else 8)
                        lastFrameChangeTime = currentTime
                    }
                }
            }

            // Invalidate to trigger a redraw
            postInvalidate()
        }
    }

    fun updateMe( currentPos : Float){
        lastPos += currentPos

        playerPosition = ((xAxis + lastPos) / (viewWidth / 5)).toInt()

        if (playerPosition < 0) {
            playerPosition = 0
        } else if (playerPosition > 3) {
            playerPosition = 3
        }

        xAxis+= lastPos
        val obstacleWidth = viewWidth / 5

        if (xAxis> (viewWidth - obstacleWidth - 100)){
            xAxis= (viewWidth - obstacleWidth - 100).toFloat()
            lastPos = 0F
            if (noBorder){
                noBorder = false
            }
        }

        else if(xAxis< (0)){
            xAxis= 0F
            lastPos = 0F
            if (noBorder){
                noBorder= false
            }
        }
        else{ noBorder = true }

        invalidate()
    }

    private fun drawEnemy(canvas: Canvas) {
        val enemyWidth = viewWidth / 4
        val enemyHeight = enemyWidth + 40
        val enemyHeightInt = enemyHeight.toInt()
        val enemyWidthInt = enemyWidth.toInt()

        if (enemyPosition < viewHeight / 2 - enemyHeightInt / 2) {
            enemyPosition += enemySlideSpeed
        } else {
            enemyPosition = (viewHeight / 2 - enemyHeightInt / 2)
            postDelayed({
                if (!isTransitionInitiated) {
                    isTransitionInitiated = true
                    handler.post {
                        val intent = Intent(context, BattleMode::class.java)
                        intent.putExtra("remainingLives", lives)
                        context.startActivity(intent)
                        (context as Activity).finish()
                    }
                }
            }, 3000)
            isEnemyMiddle = true
            val paint = Paint()
            paint.color = Color.RED
            paint.textSize = 100f
            paint.typeface = ResourcesCompat.getFont(context, R.font.bitfantasy)
            val text = "Prepare to fight"
            val textWidth = paint.measureText(text)
            val xx = (viewWidth - textWidth) / 2
            val yy = viewHeight / 4
            canvas.drawText(text, xx.toFloat(), yy.toFloat(), paint)
        }

        val currentEnemyFrame = enemyAnimationFrames[enemyFrameIndex]
        currentEnemyFrame.setBounds(
            (viewWidth / 2 - enemyWidthInt + 25).toInt(),
            enemyPosition,
            (viewWidth / 2 + enemyWidthInt- 25).toInt(),
            enemyPosition + enemyHeightInt
        )
        currentEnemyFrame.draw(canvas)

        val currentTime = System.currentTimeMillis()
        if (currentTime - enemyLastFrameChangeTime > enemyFrameDuration) {
            enemyFrameIndex = (enemyFrameIndex + 1) % enemyAnimationFrames.size
            enemyLastFrameChangeTime = currentTime
        }
    }

}
