package com.example.tp_minijeu

import android.graphics.Canvas
import android.graphics.Path
import kotlin.math.abs

/** La piste : traces, obstacles, gerbes, bonhomme et neige qui tombe. */
internal fun GameView.drawScene(c: Canvas) {
    c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paints.bg)

    drawTrack(c, -14f * s)
    drawTrack(c, 14f * s)

    // Obstacles derrière le bonhomme, puis devant (effet de profondeur)
    val limit = baseY + 55f * s
    for (o in obstacles) if (o.y <= limit) drawObstacleWrapped(c, o)

    for (p in particles) {
        paints.spray.alpha = (p.life / 0.5f * 220).toInt().coerceIn(0, 255)
        c.drawCircle(p.x, p.y, 2.2f * d, paints.spray)
    }

    // Bonhomme (+ sa copie de l'autre côté quand il passe un bord)
    val lean = (velocity / speedFactor * 2.5f).coerceIn(-20f, 20f)
    for (ox in floatArrayOf(0f, -width.toFloat(), width.toFloat())) {
        val cx = posX + ox
        if (cx < -60f * d || cx > width + 60f * d) continue
        drawPlayer(c, cx, baseY, lean)
    }

    for (o in obstacles) if (o.y > limit) drawObstacleWrapped(c, o)

    drawFallingSnow(c)
}

internal fun GameView.drawPlayer(c: Canvas, cx: Float, y: Float, lean: Float) {
    c.drawOval(cx - 22f * s, y + 5f * s, cx + 48f * s, y + 62f * s, paints.shadow)
    c.save()
    c.rotate(lean, cx, y + 20f * s)
    c.scale(taille, taille, cx, y)
    drawSnowman(c, cx, y)
    c.restore()
}

internal fun GameView.drawFallingSnow(c: Canvas) {
    for (f in airFlakes) {
        c.drawCircle(f.x, f.y, f.r, paints.flakeFill)
        c.drawCircle(f.x, f.y, f.r, paints.flakeEdge)
    }
}

/** Traces des skis dans la neige. */
internal fun GameView.drawTrack(c: Canvas, offset: Float) {
    if (trail.size < 2) return
    val p = Path()
    var prevX = 0f
    trail.forEachIndexed { i, pt ->
        // On coupe la trace quand le bonhomme passe d'un bord à l'autre
        if (i == 0 || abs(pt.x - prevX) > width / 2f) p.moveTo(pt.x + offset, pt.y)
        else p.lineTo(pt.x + offset, pt.y)
        prevX = pt.x
    }
    c.drawPath(p, paints.track)
}
