package com.example.tp_minijeu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View

enum class GameState {
    START,
    PLAYING,
    PAUSED,
    GAME_OVER
}

class GameView(context: Context?) : View(context) {
    private val paint = Paint()

    private val paintText = Paint()
    private var gameState = GameState.START
    private var ballX = 200f
    private var ballY = 300f
    private val radius = 40f

    private var speedX = 5f

    private var speedY = 8f

    init {
        paint.color = Color.RED
        paintText.color = Color.BLUE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (gameState) {
            GameState.START -> drawStartScreen(canvas)
            GameState.PLAYING -> drawGame(canvas)
            GameState.PAUSED -> drawPauseScreen(canvas)
            GameState.GAME_OVER -> drawGameOverScreen(canvas)
        }


    }

    private fun drawStartScreen(canvas: Canvas) {
        paintText.textSize = 60F
        paintText.textAlign = Paint.Align.CENTER
        canvas.drawText("MINI-JEU", width / 2f, height * 0.3F, paintText)
        canvas.drawText("Toucher pour jouer", width / 2f, height * 0.5F, paintText)
    }

    private fun drawGame(canvas: Canvas) {
        canvas.drawCircle(ballX, ballY, radius, paint)
    }

    private fun drawPauseScreen(canvas: Canvas) {
        paintText.textSize = 60F
        paintText.textAlign = Paint.Align.CENTER
        canvas.drawText("PAUSE", width / 2f, height * 0.3F, paintText)
        paintText.textSize = 40F
        canvas.drawText("Toucher pour reprendre", width / 2f, height * 0.5F, paintText)
    }

    private fun drawGameOverScreen(canvas: Canvas) {
        paintText.textSize = 60F
        paintText.textAlign = Paint.Align.CENTER
        canvas.drawText("GAME OVER", width / 2f, height * 0.3F, paintText)
        canvas.drawText("Toucher pour rejouer", width / 2f, height * 0.5F, paintText)
    }

    private fun update() {
        if (gameState == GameState.PLAYING) {
            ballX += speedX
            ballY += speedY
            if (ballX > width - radius || ballX < radius) {
                speedX *= -1f
            }

            if (ballY > height - radius && ballX < width / 2) {
                gameState = GameState.GAME_OVER
            } else if (ballY > height - radius && ballX > width / 2) {
                gameState = GameState.GAME_OVER
            }

            if (ballY > height - radius || ballY < radius) {
                speedY *= -1f
            }
        }
    }

    private val gameLoop: Runnable = object : Runnable {
        override fun run() {
            update()
            invalidate() // demande un nouveau dessin
            postDelayed(this, 16) // environ 60 mises à jour/s
        }
    }

    fun startGame() {
        post(gameLoop)
    }

    fun stopGame() {
        removeCallbacks(gameLoop)
    }

    fun resetGame() {
        gameState = GameState.START
        ballX = 200f
        ballY = 300f
        speedX = 5f
        speedY = 8f
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (gameState) {
            GameState.START -> {
                if (event.action == MotionEvent.ACTION_DOWN){
                    gameState = GameState.PLAYING
                    invalidate()
                }
            }
            GameState.PLAYING -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    ballX = event.x
                    ballY = event.y
                    gameState = GameState.PAUSED
                }
            }
            GameState.PAUSED -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    gameState = GameState.PLAYING
                }
            }
            GameState.GAME_OVER -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    resetGame()
                }
            }
        }
        return true
    }
}