package com.example.tp_minijeu.view

import com.example.tp_minijeu.controller.*
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader

// ======================= BONHOMME DE NEIGE =======================
internal fun GameView.shadedBall(c: Canvas, cx: Float, cy: Float, r: Float) {
    paints.ball.shader = RadialGradient(
        cx - 0.35f * r, cy - 0.4f * r, 1.4f * r,
        intArrayOf(Color.WHITE, Color.parseColor("#F1F6FA"), Color.parseColor("#C5D5E4")),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP
    )
    c.drawCircle(cx, cy, r, paints.ball)
    c.drawCircle(cx, cy, r, paints.ballEdge)
}

/**
 * Le bonhomme entier, (x, y) = centre de la boule du bas.
 * Chaque morceau a sa propre fonction : l'animation de chute (SnowmanCrash.kt) les dessine séparément.
 */
internal fun GameView.drawSnowman(c: Canvas, x: Float, y: Float) {
    drawSki(c, x, y, -1)
    drawSki(c, x, y, 1)
    drawBottomBall(c, x, y)
    drawMiddleBall(c, x, y)
    drawHead(c, x, y)
    drawScarf(c, x, y)
    drawHat(c, x, y)
}

/** Un ski : side = -1 gauche, 1 droite. */
internal fun GameView.drawSki(c: Canvas, x: Float, y: Float, side: Int) {
    val sx = x + side * 14f * d
    c.drawRoundRect(RectF(sx - 5f * d, y - 15f * d, sx + 5f * d, y + 55f * d), 5f * d, 5f * d, paints.ski)
    c.drawLine(sx - 1.5f * d, y - 10f * d, sx - 1.5f * d, y + 48f * d, paints.skiShine)
    c.drawRect(sx - 5.5f * d, y + 8f * d, sx + 5.5f * d, y + 14f * d, paints.binding)
}

internal fun GameView.drawBottomBall(c: Canvas, x: Float, y: Float) {
    shadedBall(c, x, y, 30f * d)
}

/** Boule du milieu avec ses boutons en charbon. */
internal fun GameView.drawMiddleBall(c: Canvas, x: Float, y: Float) {
    shadedBall(c, x, y - 42f * d, 22f * d)
    for (by in floatArrayOf(y - 48f * d, y - 38f * d)) {
        c.drawCircle(x, by, 2.6f * d, paints.coal)
        c.drawCircle(x - 0.8f * d, by - 0.8f * d, 0.8f * d, paints.glint)
    }
}

/** Tête : boule, joues, yeux, sourire et nez carotte. */
internal fun GameView.drawHead(c: Canvas, x: Float, y: Float) {
    shadedBall(c, x, y - 82f * d, 17f * d)

    // Joues
    c.drawCircle(x - 10f * d, y - 78f * d, 3.5f * d, paints.cheek)
    c.drawCircle(x + 10f * d, y - 78f * d, 3.5f * d, paints.cheek)

    // Yeux
    for (ex in floatArrayOf(x - 6f * d, x + 6f * d)) {
        c.drawCircle(ex, y - 87f * d, 2.6f * d, paints.coal)
        c.drawCircle(ex - 0.8f * d, y - 87.8f * d, 0.8f * d, paints.glint)
    }

    // Sourire
    for (i in -2..2) {
        val mx = x + i * 3.5f * d
        val my = y - 74f * d + (4 - i * i) * 0.5f * d
        c.drawCircle(mx, my, 1.2f * d, paints.coal)
    }

    // Nez carotte
    val nose = Path().apply {
        moveTo(x, y - 83f * d); lineTo(x, y - 77f * d); lineTo(x + 14f * d, y - 79.5f * d); close()
    }
    c.drawPath(nose, paints.carrot)
    c.drawLine(x + 4f * d, y - 81.5f * d, x + 4f * d, y - 78.5f * d, paints.carrotLine)
    c.drawLine(x + 8f * d, y - 80.8f * d, x + 8f * d, y - 78.8f * d, paints.carrotLine)
}

/** Écharpe et son pan (entre la tête et la boule du milieu). */
internal fun GameView.drawScarf(c: Canvas, x: Float, y: Float) {
    c.drawRoundRect(RectF(x - 18f * d, y - 67f * d, x + 18f * d, y - 58f * d), 4f * d, 4f * d, paints.scarf)
    c.drawRoundRect(RectF(x - 17f * d, y - 61f * d, x + 17f * d, y - 58.5f * d), 2f * d, 2f * d, paints.scarfDark)
    c.drawRect(x - 9f * d, y - 67f * d, x - 6f * d, y - 58f * d, paints.scarfStripe)
    c.drawRect(x - 1f * d, y - 67f * d, x + 2f * d, y - 58f * d, paints.scarfStripe)
    // Pan de l'écharpe
    c.drawRoundRect(RectF(x + 5f * d, y - 62f * d, x + 13f * d, y - 43f * d), 3f * d, 3f * d, paints.scarf)
    c.drawRect(x + 5f * d, y - 52f * d, x + 13f * d, y - 49f * d, paints.scarfStripe)
    for (fx in floatArrayOf(6.5f, 9f, 11.5f)) {
        c.drawLine(x + fx * d, y - 43f * d, x + fx * d, y - 39f * d, paints.scarfLine)
    }
}

/** Chapeau haut-de-forme. */
internal fun GameView.drawHat(c: Canvas, x: Float, y: Float) {
    c.drawRoundRect(RectF(x - 12f * d, y - 120f * d, x + 12f * d, y - 98f * d), 2f * d, 2f * d, paints.hat)
    c.drawRect(x - 12f * d, y - 104f * d, x + 12f * d, y - 100f * d, paints.hatBand)
    c.drawRoundRect(RectF(x - 20f * d, y - 100f * d, x + 20f * d, y - 95f * d), 2.5f * d, 2.5f * d, paints.hat)
    c.drawLine(x - 8f * d, y - 117f * d, x - 8f * d, y - 106f * d, paints.hatShine)
}
