package com.example.tp_minijeu

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

internal fun GameView.drawCard(c: Canvas, r: RectF) {
    val rad = 24f * d
    tmpRect.set(r.left, r.top + 4f * d, r.right, r.bottom + 4f * d)
    c.drawRoundRect(tmpRect, rad, rad, paints.cardShadow)
    c.drawRoundRect(r, rad, rad, paints.card)
}

internal fun drawButton(c: Canvas, r: RectF, label: String, fill: Paint, textPaint: Paint) {
    val rad = r.height() / 2f
    c.drawRoundRect(r, rad, rad, fill)
    // Centrage vertical : y de drawText = ligne de base, on la décale de la moitié de la hauteur du texte
    val ty = r.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
    c.drawText(label, r.centerX(), ty, textPaint)
}
