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

private val gameState = GameState.START

class GameView(context: Context?) : View(context) {
    private val paint = Paint()
    private var ballX = 200f
    private var ballY = 300f
    private val radius = 40f

    private var speedX = 5f

    private var speedY = 8f

    init {
        paint.color = Color.RED
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
        canvas.drawText("MINI-JEU", 100F, 200F, paint);
        canvas.drawText("Toucher pour jouer", 100F, 300F, paint);
    }
    private fun update() {
        if (gameState == GameState.PLAYING) {
            ballX += speedX
            ballY += speedY
        }
    }

    private val gameLoop: Runnable = object : Runnable {
        override fun run() {
            update()
            invalidate() // demande un nouveau dessin
            postDelayed(this, 16) // environ 15 mises à jour/s
            if (ballX > width - radius || ballX < radius) {
                speedX *= -1f
            }

            if (ballY > height - radius || ballY < radius) {
                speedY *= -1f
            }

        }
    }

    fun startGame() {
        post(gameLoop)
    }

    fun stopGame() {
        removeCallbacks(gameLoop)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            ballX = event.x
            ballY = event.y
            return true
        }
        return true
    }
}