package com.example.tp_minijeu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.view.View
import kotlin.random.Random

class GameView(context: Context) : View(context) {

    var tilt = 0f

    private val d = resources.displayMetrics.density   // 1dp en pixels
    private val taille = 0.6f                          // taille du bonhomme (1f = taille d'origine)

    private var posX = -1f
    private var lastTime = 0L

    private val speedFactor = 45f * d     // vitesse gauche/droite
    private val scrollSpeed = 220f * d    // vitesse de descente (défilement)

    private val trail = ArrayDeque<PointF>()
    private val flakes = MutableList(40) { PointF() }

    // ---------- Pinceaux ----------
    private fun fill(c: String) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(c); style = Paint.Style.FILL
    }
    private fun line(c: String, w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(c); style = Paint.Style.STROKE
        strokeWidth = w; strokeCap = Paint.Cap.ROUND
    }

    private val snow = fill("#FFFFFF")
    private val snowOutline = line("#9FB6CC", 2f * d)
    private val black = fill("#222222")
    private val carrot = fill("#FF8C1A")
    private val scarf = fill("#D32F2F")
    private val ski = fill("#1565C0")
    private val track = line("#D0DEEA", 3f * d)
    private val flake = fill("#DCE7F1")

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        flakes.forEach { it.set(Random.nextFloat() * w, Random.nextFloat() * h) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#F4F8FC"))

        val now = System.nanoTime()
        val dt = if (lastTime == 0L) 0f
        else ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
        lastTime = now

        if (posX < 0) posX = width / 2f
        val velocity = -tilt * speedFactor
        posX += velocity * dt
        val margin = 30f * d
        posX = posX.coerceIn(margin, maxOf(margin, width - margin))

        // Position verticale : quart haut de l'écran
        val baseY = maxOf(height * 0.25f, 80f * d)

        // Flocons au sol qui défilent vers le haut
        for (f in flakes) {
            f.y -= scrollSpeed * dt
            if (f.y < 0) { f.y += height; f.x = Random.nextFloat() * width }
            canvas.drawCircle(f.x, f.y, 2.5f * d, flake)
        }

        // Traces de skis
        trail.forEach { it.y -= scrollSpeed * dt }
        while (trail.isNotEmpty() && trail.first().y < -20f) trail.removeFirst()
        trail.addLast(PointF(posX, baseY - 15f * d * taille))
        drawTrack(canvas, -14f * d * taille)
        drawTrack(canvas, 14f * d * taille)

        // Le bonhomme se penche dans le sens du mouvement, et il est réduit
        val lean = (velocity / speedFactor * 2.5f).coerceIn(-20f, 20f)
        canvas.save()
        canvas.rotate(lean, posX, baseY + 20f * d * taille)
        canvas.scale(taille, taille, posX, baseY)
        drawSnowman(canvas, posX, baseY)
        canvas.restore()

        postInvalidateOnAnimation()
    }

    private fun drawTrack(c: Canvas, offset: Float) {
        if (trail.size < 2) return
        val p = Path()
        trail.forEachIndexed { i, pt ->
            if (i == 0) p.moveTo(pt.x + offset, pt.y) else p.lineTo(pt.x + offset, pt.y)
        }
        c.drawPath(p, track)
    }

    private fun drawSnowman(c: Canvas, x: Float, y: Float) {
        // Skis
        c.drawRoundRect(RectF(x - 19f * d, y - 15f * d, x - 9f * d, y + 55f * d), 5f * d, 5f * d, ski)
        c.drawRoundRect(RectF(x + 9f * d, y - 15f * d, x + 19f * d, y + 55f * d), 5f * d, 5f * d, ski)

        // Corps : boule du bas puis boule du milieu
        c.drawCircle(x, y, 30f * d, snow);              c.drawCircle(x, y, 30f * d, snowOutline)
        c.drawCircle(x, y - 42f * d, 22f * d, snow);    c.drawCircle(x, y - 42f * d, 22f * d, snowOutline)

        // Boutons
        c.drawCircle(x, y - 46f * d, 2.5f * d, black)
        c.drawCircle(x, y - 36f * d, 2.5f * d, black)

        // Écharpe
        c.drawRoundRect(RectF(x - 18f * d, y - 67f * d, x + 18f * d, y - 58f * d), 4f * d, 4f * d, scarf)
        c.drawRoundRect(RectF(x + 5f * d, y - 62f * d, x + 13f * d, y - 44f * d), 3f * d, 3f * d, scarf)

        // Tête
        c.drawCircle(x, y - 82f * d, 17f * d, snow);    c.drawCircle(x, y - 82f * d, 17f * d, snowOutline)

        // Yeux
        c.drawCircle(x - 6f * d, y - 86f * d, 2.3f * d, black)
        c.drawCircle(x + 6f * d, y - 86f * d, 2.3f * d, black)

        // Nez carotte
        val nose = Path().apply {
            moveTo(x, y - 82f * d)
            lineTo(x, y - 77f * d)
            lineTo(x + 13f * d, y - 79f * d)
            close()
        }
        c.drawPath(nose, carrot)

        // Chapeau
        c.drawRect(x - 19f * d, y - 99f * d, x + 19f * d, y - 95f * d, black)
        c.drawRect(x - 12f * d, y - 118f * d, x + 12f * d, y - 98f * d, black)
    }
}