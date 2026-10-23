package com.example.tp_minijeu

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader

/**
 * Skin secret (easter egg : toucher le titre sur l'accueil).
 * Objet global : garde sa valeur tant que l'appli tourne, même si l'écran est recréé.
 * Pas sauvegardé : au prochain lancement, on repart avec le bonhomme de neige.
 */
internal object SantaSkin {
    var enabled = false
}

// ======================= PÈRE NOËL DANS SON TRAÎNEAU (skin secret) =======================
// Mêmes repères que le bonhomme : (x, y) = centre de la boule du bas, unités en dp (x d).
// Il tient dans les mêmes dimensions : de y-100 (pompon) à y+56 (patins), largeur +/- 30dp.
// Gros Père Noël (ventre de 26dp de rayon) dans un petit traîneau (+/- 22dp) : le ventre déborde.

/** Le Père Noël entier. Chaque morceau a sa fonction : l'animation de fin les dessine séparément. */
internal fun GameView.drawSantaSleigh(c: Canvas, x: Float, y: Float) {
    drawSleighRunner(c, x, y, -1)
    drawSleighRunner(c, x, y, 1)
    drawSleighBack(c, x, y)
    drawGift(c, x, y, 0)
    drawGift(c, x, y, 1)
    drawSanta(c, x, y)
    drawSantaHat(c, x, y)
    drawSleighFront(c, x, y)
}

/** Patin doré du traîneau, enroulé à l'avant. side = -1 gauche, 1 droite. */
internal fun GameView.drawSleighRunner(c: Canvas, x: Float, y: Float, side: Int) {
    val sx = x + side * 15f * d
    c.drawRoundRect(RectF(sx - 3.5f * d, y - 14f * d, sx + 3.5f * d, y + 50f * d), 3.5f * d, 3.5f * d, paints.runner)
    c.drawLine(sx - 1.2f * d, y - 10f * d, sx - 1.2f * d, y + 44f * d, paints.runnerShine)
    c.drawCircle(sx, y + 50f * d, 5.5f * d, paints.runnerCurl)
}

/** Dossier du traîneau, derrière le Père Noël. */
internal fun GameView.drawSleighBack(c: Canvas, x: Float, y: Float) {
    c.drawRoundRect(RectF(x - 21f * d, y - 38f * d, x + 21f * d, y + 2f * d), 9f * d, 9f * d, paints.sleighBack)
    c.drawRoundRect(RectF(x - 22f * d, y - 41f * d, x + 22f * d, y - 35f * d), 3f * d, 3f * d, paints.gold)
}

/** Cadeau qui dépasse derrière une épaule : i = 0 (vert, à gauche) ou 1 (bleu, à droite). */
internal fun GameView.drawGift(c: Canvas, x: Float, y: Float, i: Int) {
    val gx = x + (if (i == 0) -19f else 19f) * d
    val gy = y + (if (i == 0) -50f else -54f) * d
    val h = (if (i == 0) 6.5f else 7f) * d
    c.drawRect(gx - h, gy - h, gx + h, gy + h, paints.giftColors[i])
    val ribbon = paints.ribbonColors[i]
    c.drawRect(gx - 1.3f * d, gy - h, gx + 1.3f * d, gy + h, ribbon)
    c.drawRect(gx - h, gy - 1.3f * d, gx + h, gy + 1.3f * d, ribbon)
    c.drawCircle(gx - 2.5f * d, gy - h - 1.5f * d, 2.5f * d, ribbon)        // le nœud
    c.drawCircle(gx + 2.5f * d, gy - h - 1.5f * d, 2.5f * d, ribbon)
}

/** Le Père Noël : bras, gros ventre rouge, ceinture, tête, barbe et moustache. */
internal fun GameView.drawSanta(c: Canvas, x: Float, y: Float) {
    // Bras : manches, revers de fourrure et moufles posées sur le bord du traîneau
    for (side in intArrayOf(-1, 1)) {
        val ax = x + side * 24f * d
        c.drawRoundRect(RectF(ax - 6f * d, y - 38f * d, ax + 6f * d, y - 12f * d), 6f * d, 6f * d, paints.santaRedDark)
        c.drawRoundRect(RectF(ax - 6f * d, y - 16f * d, ax + 6f * d, y - 10f * d), 3f * d, 3f * d, paints.fur)
        c.drawCircle(ax, y - 6f * d, 5f * d, paints.mitten)
    }

    // Gros ventre rouge ombré
    val by = y - 22f * d
    val r = 26f * d
    paints.santaBelly.shader = RadialGradient(
        x - 0.35f * r, by - 0.4f * r, 1.4f * r,
        intArrayOf(Color.parseColor("#EF5350"), Color.parseColor("#D32F2F"), Color.parseColor("#9A1B1B")),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP
    )
    c.drawCircle(x, by, r, paints.santaBelly)

    // Bande de fourrure, ceinture et boucle dorée (coupées au bord du ventre)
    c.save()
    c.clipPath(Path().apply { addCircle(x, by, r, Path.Direction.CW) })
    c.drawRect(x - 3.5f * d, by - r, x + 3.5f * d, by + r, paints.fur)
    c.drawRect(x - r, y - 21f * d, x + r, y - 14f * d, paints.belt)
    c.restore()
    c.drawRect(x - 5f * d, y - 22f * d, x + 5f * d, y - 13f * d, paints.goldLine)

    // Tête : visage, barbe (avec ses boucles), sourcils, yeux, moustache et nez
    c.drawCircle(x, y - 56f * d, 13f * d, paints.skin)
    c.drawOval(RectF(x - 15f * d, y - 60f * d, x + 15f * d, y - 32f * d), paints.fur)
    c.drawCircle(x - 8f * d, y - 37f * d, 6f * d, paints.fur)
    c.drawCircle(x, y - 34f * d, 7f * d, paints.fur)
    c.drawCircle(x + 8f * d, y - 37f * d, 6f * d, paints.fur)
    c.drawOval(RectF(x - 15f * d, y - 60f * d, x + 15f * d, y - 32f * d), paints.furEdge)
    c.drawRoundRect(RectF(x - 8.5f * d, y - 66f * d, x - 2.5f * d, y - 63.5f * d), d, d, paints.fur)
    c.drawRoundRect(RectF(x + 2.5f * d, y - 66f * d, x + 8.5f * d, y - 63.5f * d), d, d, paints.fur)
    for (ex in floatArrayOf(x - 5f * d, x + 5f * d)) {
        c.drawCircle(ex, y - 60.5f * d, 1.9f * d, paints.coal)
        c.drawCircle(ex - 0.6f * d, y - 61.1f * d, 0.6f * d, paints.glint)
    }
    c.drawCircle(x - 9.5f * d, y - 57.5f * d, 2.5f * d, paints.cheek)
    c.drawCircle(x + 9.5f * d, y - 57.5f * d, 2.5f * d, paints.cheek)
    for (side in intArrayOf(-1, 1)) {
        val m = RectF(x + minOf(0f, side * 10f) * d, y - 56f * d, x + maxOf(0f, side * 10f) * d, y - 49.5f * d)
        c.drawOval(m, paints.fur)
        c.drawOval(m, paints.furEdge)
    }
    c.drawCircle(x, y - 56.5f * d, 3.2f * d, paints.santaNose)
}

/** Bonnet rouge qui retombe sur le côté, avec sa bordure de fourrure et son pompon. */
internal fun GameView.drawSantaHat(c: Canvas, x: Float, y: Float) {
    val cone = Path().apply {
        moveTo(x - 12f * d, y - 71f * d)
        cubicTo(x - 10f * d, y - 88f * d, x + 2f * d, y - 98f * d, x + 15f * d, y - 96f * d)
        cubicTo(x + 8f * d, y - 92f * d, x + 10f * d, y - 82f * d, x + 12f * d, y - 71f * d)
        close()
    }
    c.drawPath(cone, paints.santaRed)
    val band = RectF(x - 15f * d, y - 75f * d, x + 15f * d, y - 67f * d)
    c.drawRoundRect(band, 4f * d, 4f * d, paints.fur)
    c.drawRoundRect(band, 4f * d, 4f * d, paints.furEdge)
    c.drawCircle(x + 16f * d, y - 96f * d, 4.5f * d, paints.fur)
}

/** Avant du traîneau, devant les jambes du Père Noël : rouge, bord doré et ornements. */
internal fun GameView.drawSleighFront(c: Canvas, x: Float, y: Float) {
    val body = RectF(x - 22f * d, y - 4f * d, x + 22f * d, y + 32f * d)
    c.drawRoundRect(body, 10f * d, 10f * d, paints.sleighFront)
    c.save()
    c.clipRect(x - 22f * d, y + 23f * d, x + 22f * d, y + 32f * d)
    c.drawRoundRect(body, 10f * d, 10f * d, paints.sleighShade)        // dessous plus sombre
    c.restore()
    c.drawRoundRect(RectF(x - 23f * d, y - 7f * d, x + 23f * d, y - 1f * d), 3f * d, 3f * d, paints.gold)
    c.drawRoundRect(RectF(x - 17f * d, y + 3f * d, x + 17f * d, y + 26f * d), 7f * d, 7f * d, paints.goldLine)
    c.drawCircle(x, y + 14.5f * d, 4.5f * d, paints.goldLine)
    c.drawCircle(x - 8.5f * d, y + 14.5f * d, 2.5f * d, paints.goldLine)
    c.drawCircle(x + 8.5f * d, y + 14.5f * d, 2.5f * d, paints.goldLine)
}
