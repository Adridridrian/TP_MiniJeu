package com.example.tp_minijeu.view

import com.example.tp_minijeu.controller.*
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

// Longueur des stalactites (dp), reprises en boucle sous les lettres
private val ICICLE_LENGTHS = floatArrayOf(20f, 14f, 24f, 16f, 18f, 13f)
// Lettres dont le bas est plein : une stalactite peut y pendre sans se voir entre deux jambages
private const val ICICLE_LETTERS = "BCDEGIJLOQSUZ"

/**
 * Titre en bloc de glace, centré en cx sur la ligne de base "baseline" :
 * stalactites sous les lettres, épaisseur 3D, contour bleu nuit, dégradé glacé et étincelles.
 */
internal fun GameView.drawIceTitle(c: Canvas, text: String, cx: Float, baseline: Float) {
    val fill = paints.iceFill
    val capTop = baseline - fill.textSize * 0.72f      // haut des majuscules

    // Stalactites derrière les lettres (leur haut est caché) : seulement sous les lettres à base pleine,
    // sinon on les voit entre les jambages (N, R, H...), et jamais sous deux lettres voisines
    val widths = FloatArray(text.length)
    fill.getTextWidths(text, widths)
    var x = cx - widths.sum() / 2f
    var previous = false
    text.forEachIndexed { i, ch ->
        val hang = ch in ICICLE_LETTERS && !previous
        if (hang) drawIcicle(c, x + widths[i] / 2f, baseline, ICICLE_LENGTHS[i % ICICLE_LENGTHS.size] * d)
        previous = hang
        x += widths[i]
    }

    // Épaisseur du bloc : la même forme, décalée vers le bas, en bleu très foncé
    c.drawText(text, cx + 1.5f * d, baseline + 4f * d, paints.iceDepth)
    // Contour, puis remplissage en dégradé blanc -> bleu glace
    c.drawText(text, cx, baseline, paints.iceOutline)
    fill.shader = LinearGradient(
        0f, capTop, 0f, baseline,
        intArrayOf(Color.WHITE, Color.parseColor("#D8F1FF"), Color.parseColor("#7CC2F5")),
        floatArrayOf(0f, 0.45f, 1f),
        Shader.TileMode.CLAMP
    )
    c.drawText(text, cx, baseline, fill)

    // Étincelles sur les coins du bloc
    val half = fill.measureText(text) / 2f
    drawSparkle(c, cx + half - 4f * d, capTop + 2f * d, 7f * d)
    drawSparkle(c, cx - half + 10f * d, capTop + 14f * d, 4.5f * d)
}

/** Stalactite qui pend sous la ligne de base, en x. */
private fun GameView.drawIcicle(c: Canvas, x: Float, baseline: Float, len: Float) {
    val w = 6f * d
    val path = Path().apply {
        moveTo(x - w, baseline - 4f * d)
        lineTo(x + w, baseline - 4f * d)
        quadTo(x + w * 0.35f, baseline + len * 0.5f, x, baseline + len)
        quadTo(x - w * 0.35f, baseline + len * 0.5f, x - w, baseline - 4f * d)
        close()
    }
    c.drawPath(path, paints.icicleFill)
    c.drawPath(path, paints.icicleEdge)
}

/** Petite étoile à 4 branches (blanche par défaut). */
internal fun GameView.drawSparkle(c: Canvas, x: Float, y: Float, r: Float, paint: Paint = paints.sparkle) {
    val k = r * 0.22f
    val path = Path().apply {
        moveTo(x, y - r); quadTo(x + k, y - k, x + r, y)
        quadTo(x + k, y + k, x, y + r); quadTo(x - k, y + k, x - r, y)
        quadTo(x - k, y - k, x, y - r); close()
    }
    c.drawPath(path, paint)
}
