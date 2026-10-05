package com.example.tp_minijeu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class GameView(context: Context?) : View(context) {
    private val paint = Paint()
    private var ballX = 200f
    private val ballY = 300f
    private val radius = 40f

    init {
        paint.color = Color.RED
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(ballX, ballY, radius, paint)
        canvas.drawRect(ballX, ballY - 200f, ballX + radius, ballY + radius, paint)
    }

    private fun update() {
        ballX += 5f
    }

    private val gameLoop: Runnable = object : Runnable {
        override fun run() {
            update()
            invalidate() // demande un nouveau dessin
            postDelayed(this, 67) // environ 15 mises à jour/s

        }
    }

    fun startGame() {
        post(gameLoop)
    }

    fun stopGame() {
        removeCallbacks(gameLoop)
    }
}