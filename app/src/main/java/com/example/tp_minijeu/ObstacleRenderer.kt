package com.example.tp_minijeu

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Dessine l'obstacle, et sa copie de l'autre côté de l'écran s'il dépasse d'un bord. */
internal fun GameView.drawObstacleWrapped(c: Canvas, o: Obstacle) {
    val w = width.toFloat()
    for (ox in floatArrayOf(0f, -w, w)) {
        val x = o.x + ox
        if (x < -2f * o.size || x > w + 2f * o.size) continue
        if (o.isTree) drawTree(c, x, o.y, o.size) else drawRock(c, o, x)
    }
}

internal fun GameView.drawTree(c: Canvas, x: Float, y: Float, s: Float) {
    // Ombre
    c.drawOval(x - 0.6f * s, y - 0.25f * s, x + 1.4f * s, y + 0.25f * s, paints.shadow)
    // Tronc
    c.drawRoundRect(RectF(x - 0.18f * s, y - 0.55f * s, x + 0.18f * s, y), 0.05f * s, 0.05f * s, paints.trunk)

    // 3 étages : (bas, haut, demi-largeur)
    val tiers = listOf(
        Triple(y - 0.35f * s, y - 1.5f * s, s),
        Triple(y - 0.95f * s, y - 1.95f * s, 0.8f * s),
        Triple(y - 1.45f * s, y - 2.4f * s, 0.6f * s)
    )
    for ((b, t, w) in tiers) {
        // Feuillage
        val leaf = Path().apply {
            moveTo(x - w, b); quadTo(x, b + 0.18f * s, x + w, b); lineTo(x, t); close()
        }
        c.drawPath(leaf, paints.treeGreen)
        // Côté à l'ombre
        val shade = Path().apply {
            moveTo(x, t); lineTo(x + w, b); quadTo(x + 0.5f * w, b + 0.12f * s, x, b + 0.09f * s); close()
        }
        c.drawPath(shade, paints.treeDark)
        // Neige sur les branches
        val h = b - t
        val snowCap = Path().apply {
            moveTo(x, t)
            lineTo(x - w * 0.42f, t + h * 0.42f)
            quadTo(x - w * 0.2f, t + h * 0.3f, x - w * 0.05f, t + h * 0.5f)
            quadTo(x + w * 0.15f, t + h * 0.32f, x + w * 0.42f, t + h * 0.42f)
            close()
        }
        c.drawPath(snowCap, paints.snowFlat)
    }
}

internal fun GameView.drawRock(c: Canvas, o: Obstacle, x: Float) {
    val y = o.y; val r = o.size
    // Ombre
    c.drawOval(x - 0.8f * r, y + 0.1f * r, x + 1.3f * r, y + 0.55f * r, paints.shadow)

    // Forme irrégulière
    val path = Path()
    for (i in 0 until 8) {
        val a = i * PI / 4
        val px = x + (cos(a) * r * o.shape[i]).toFloat()
        val py = y - 0.2f * r + (sin(a) * r * 0.65f * o.shape[i]).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()

    paints.rock.shader = LinearGradient(
        x - r, y - r, x + r, y + 0.5f * r,
        Color.parseColor("#A3ACB6"), Color.parseColor("#545C66"),
        Shader.TileMode.CLAMP
    )
    c.drawPath(path, paints.rock)
    c.drawPath(path, paints.rockEdge)

    // Neige sur le dessus
    c.save()
    c.clipPath(path)
    c.drawOval(x - 0.9f * r, y - 1.1f * r, x + 0.7f * r, y - 0.55f * r, paints.snowFlat)
    c.restore()
}
