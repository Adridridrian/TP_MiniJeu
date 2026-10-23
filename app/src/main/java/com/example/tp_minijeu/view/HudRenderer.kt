package com.example.tp_minijeu.view

import com.example.tp_minijeu.controller.*
import com.example.tp_minijeu.model.*
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

// ======================= INTERFACE EN JEU =======================
internal fun GameView.drawHud(c: Canvas) {
    // Score en haut à gauche
    val label = "${score()} m"
    val tw = paints.scoreText.measureText(label)
    val left = 16f * d
    val right = left + tw + 32f * d
    val top = 40f * d
    val bottom = 76f * d
    val rad = 18f * d
    tmpRect.set(left, top + 3f * d, right, bottom + 3f * d)
    c.drawRoundRect(tmpRect, rad, rad, paints.pillShadow)
    tmpRect.set(left, top, right, bottom)
    c.drawRoundRect(tmpRect, rad, rad, paints.pill)
    c.drawText(label, left + 16f * d, 65f * d, paints.scoreText)

    drawLives(c, top, bottom, rad)
    if (touchMode) drawControlBar(c)
    drawMicGauge(c)
}

/** Vies restantes en haut à droite : cœurs rouges, et gris pour les vies perdues. */
internal fun GameView.drawLives(c: Canvas, top: Float, bottom: Float, rad: Float) {
    val size = 18f * d                    // largeur d'un cœur
    val gap = 6f * d
    val right = width - 16f * d
    val left = right - maxLives * size - (maxLives - 1) * gap - 28f * d
    tmpRect.set(left, top + 3f * d, right, bottom + 3f * d)
    c.drawRoundRect(tmpRect, rad, rad, paints.pillShadow)
    tmpRect.set(left, top, right, bottom)
    c.drawRoundRect(tmpRect, rad, rad, paints.pill)

    val cy = (top + bottom) / 2f
    for (i in 0 until maxLives) {
        val cx = left + 14f * d + size / 2f + i * (size + gap)
        drawHeart(c, cx, cy, size, if (i < lives) paints.heartFull else paints.heartEmpty)
    }
}

/** Cœur centré en (cx, cy), de largeur size. */
internal fun drawHeart(c: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
    val w = size / 2f
    val top = cy - size * 0.35f
    val path = Path().apply {
        moveTo(cx, cy + size * 0.45f)                                     // pointe du bas
        cubicTo(cx - w * 1.6f, cy - size * 0.05f, cx - w * 0.9f, top - size * 0.45f, cx, top)
        cubicTo(cx + w * 0.9f, top - size * 0.45f, cx + w * 1.6f, cy - size * 0.05f, cx, cy + size * 0.45f)
        close()
    }
    c.drawPath(path, paint)
}

/** Mode tactile : barre en bas de l'écran avec une poignée qui suit le bonhomme. */
internal fun GameView.drawControlBar(c: Canvas) {
    val left = 16f * d
    val right = width - 16f * d
    val rad = 22f * d
    tmpRect.set(left, controlY - rad + 3f * d, right, controlY + rad + 3f * d)
    c.drawRoundRect(tmpRect, rad, rad, paints.pillShadow)
    tmpRect.set(left, controlY - rad, right, controlY + rad)
    c.drawRoundRect(tmpRect, rad, rad, paints.pill)

    // Rail
    val h = 3f * d
    tmpRect.set(left + rad, controlY - h, right - rad, controlY + h)
    c.drawRoundRect(tmpRect, h, h, paints.gaugeTrack)

    // Poignée à la position du bonhomme
    val kx = posX.coerceIn(left + rad, right - rad)
    val kr = if (steering) 18f * d else 16f * d
    c.drawCircle(kx, controlY + 2f * d, kr, paints.pillShadow)
    c.drawCircle(kx, controlY, kr, paints.thumbFill)
    c.drawCircle(kx, controlY, kr, paints.thumbEdge)
}

/** Jauge du micro en bas de l'écran : volume, barre de seuil et vitesse actuelle. */
internal fun GameView.drawMicGauge(c: Canvas) {
    val left = 16f * d
    val right = width - 16f * d
    val bottom = height - 32f * d
    val top = bottom - 44f * d
    val rad = 22f * d
    tmpRect.set(left, top + 3f * d, right, bottom + 3f * d)
    c.drawRoundRect(tmpRect, rad, rad, paints.pillShadow)
    tmpRect.set(left, top, right, bottom)
    c.drawRoundRect(tmpRect, rad, rad, paints.pill)

    // Icône micro
    val mx = left + 24f * d
    val my = (top + bottom) / 2f
    tmpRect.set(mx - 4f * d, my - 11f * d, mx + 4f * d, my + 3f * d)
    c.drawRoundRect(tmpRect, 4f * d, 4f * d, paints.hudIcon)
    tmpRect.set(mx - 8f * d, my - 6f * d, mx + 8f * d, my + 7f * d)
    c.drawArc(tmpRect, 0f, 180f, false, paints.micBody)
    c.drawLine(mx, my + 7f * d, mx, my + 11f * d, paints.micBody)

    // Vitesse actuelle, à droite (en rouge juste après une accélération)
    val speedLabel = "x%.1f".format(speedMultiplier)
    val sp = if (speedFlash > 0f) paints.speedUpText else paints.speedText
    c.drawText(speedLabel, right - 16f * d, my + 6f * d, sp)

    val barLeft = left + 44f * d
    val barRight = right - 76f * d
    if (!micAvailable) {
        c.drawText("Micro non autorisé", barLeft, my + 5f * d, paints.micOffText)
        return
    }
    drawLevelBar(c, barLeft, barRight, my, 6f * d)
}

/** Barre de niveau du micro avec repère du seuil (utilisée en jeu et dans les paramètres). */
internal fun GameView.drawLevelBar(c: Canvas, l: Float, r: Float, cy: Float, halfH: Float) {
    tmpRect.set(l, cy - halfH, r, cy + halfH)
    c.drawRoundRect(tmpRect, halfH, halfH, paints.gaugeTrack)
    val level = micDisplay.coerceIn(0f, 1f)
    if (level > 0.01f) {
        tmpRect.set(l, cy - halfH, l + (r - l) * level, cy + halfH)
        c.drawRoundRect(tmpRect, halfH, halfH, if (micDisplay > micThreshold) paints.gaugeLoud else paints.gaugeCalm)
    }
    val sx = l + (r - l) * micThreshold
    c.drawLine(sx, cy - halfH - 5f * d, sx, cy + halfH + 5f * d, paints.gaugeMark)
}
