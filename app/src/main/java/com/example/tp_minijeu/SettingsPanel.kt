package com.example.tp_minijeu

import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun GameView.saveSettings() {
    prefs.edit()
        .putFloat("moveSens", moveSens)
        .putFloat("micSens", micSens)
        .putBoolean("touchMode", touchMode)
        .apply()
}

// ======================= PANNEAU PAUSE / PARAMÈTRES =======================
internal fun GameView.drawSettingsPanel(c: Canvas, title: String, cardRect: RectF, inGame: Boolean) {
    drawCard(c, cardRect)
    val cx = width / 2f
    c.drawText(title, cx, panelTop + 50f * d, paints.titleText)

    // Mode de jeu : immersif (inclinaison) ou tactile (barre)
    c.drawText("Mode de jeu", trackLeft, panelTop + 92f * d, paints.labelText)
    if (touchMode) {
        drawButton(c, modeTiltButton, "Immersif", paints.buttonAlt, paints.segmentOffText)
        drawButton(c, modeTouchButton, "Tactile", paints.button, paints.segmentOnText)
    } else {
        drawButton(c, modeTiltButton, "Immersif", paints.button, paints.segmentOnText)
        drawButton(c, modeTouchButton, "Tactile", paints.buttonAlt, paints.segmentOffText)
    }

    // Sensibilité du mouvement
    c.drawText("Sensibilité mouvement", trackLeft, moveY - 22f * d, paints.labelText)
    c.drawText(percent(moveSens), trackRight, moveY - 22f * d, paints.valueText)
    drawSlider(c, moveY, (moveSens - moveMin) / (moveMax - moveMin))

    // Sensibilité du micro + jauge de test en direct
    c.drawText("Sensibilité micro", trackLeft, micY - 22f * d, paints.labelText)
    c.drawText(percent(micSens), trackRight, micY - 22f * d, paints.valueText)
    drawSlider(c, micY, (micSens - micMin) / (micMax - micMin))
    if (micAvailable) {
        drawLevelBar(c, trackLeft, trackRight, previewY, 4f * d)
    } else {
        c.drawText("Micro non autorisé", trackLeft, previewY + 5f * d, paints.micOffText)
    }

    // Valeurs par défaut
    val ty = defaultsButton.centerY() - (paints.linkText.ascent() + paints.linkText.descent()) / 2f
    c.drawText("Valeurs par défaut", cx, ty, paints.linkText)

    if (inGame) {
        drawButton(c, primaryButton, "Reprendre", paints.button, paints.buttonText)
        drawButton(c, secondaryButton, "Menu", paints.buttonAlt, paints.buttonAltText)
    } else {
        drawButton(c, primaryButton, "Retour", paints.button, paints.buttonText)
    }
}

internal fun percent(v: Float) = "${(v * 100).roundToInt()} %"

/** Curseur : barre, partie remplie et poignée ronde. t entre 0 et 1. */
internal fun GameView.drawSlider(c: Canvas, y: Float, t: Float) {
    val tt = t.coerceIn(0f, 1f)
    val h = 3f * d
    tmpRect.set(trackLeft, y - h, trackRight, y + h)
    c.drawRoundRect(tmpRect, h, h, paints.gaugeTrack)
    val tx = trackLeft + (trackRight - trackLeft) * tt
    tmpRect.set(trackLeft, y - h, tx, y + h)
    c.drawRoundRect(tmpRect, h, h, paints.sliderFill)
    c.drawCircle(tx, y + 2f * d, 12f * d, paints.pillShadow)
    c.drawCircle(tx, y, 12f * d, paints.thumbFill)
    c.drawCircle(tx, y, 12f * d, paints.thumbEdge)
}

internal fun GameView.hitSlider(sliderY: Float, x: Float, y: Float) =
    abs(y - sliderY) < 24f * d && x > trackLeft - 24f * d && x < trackRight + 24f * d

internal fun GameView.setSliderFromX(which: Int, x: Float) {
    val t = ((x - trackLeft) / (trackRight - trackLeft)).coerceIn(0f, 1f)
    if (which == 1) {
        var v = moveMin + t * (moveMax - moveMin)
        if (abs(v - 1f) < 0.05f) v = 1f        // se cale sur 100 % (réglage de base)
        moveSens = v
    } else {
        var v = micMin + t * (micMax - micMin)
        if (abs(v - 1f) < 0.05f) v = 1f
        micSens = v
    }
}
