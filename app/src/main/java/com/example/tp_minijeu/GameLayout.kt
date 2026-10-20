package com.example.tp_minijeu

import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import kotlin.random.Random

/** Appelé quand la taille de l'écran change : fond, neige et zones des boutons / du panneau. */
internal fun GameView.setupLayout(w: Int, h: Int) {
    paints.bg.shader = LinearGradient(
        0f, 0f, 0f, h.toFloat(),
        Color.parseColor("#D3E4F3"), Color.parseColor("#F7FAFD"),
        Shader.TileMode.CLAMP
    )
    airFlakes.clear()
    repeat(45) {
        airFlakes.add(Flake(
            Random.nextFloat() * w, Random.nextFloat() * h,
            (1.5f + Random.nextFloat() * 2.5f) * d,
            (25f + Random.nextFloat() * 45f) * d,
            Random.nextFloat() * 6.28f
        ))
    }

    baseY = maxOf(h * 0.25f, 80f * d)
    if (posX < 0f) posX = w / 2f

    val cx = w / 2f
    val cy = h * 0.45f
    val hw = minOf(160f * d, w / 2f - 20f * d)
    val bw = minOf(140f * d, w / 2f - 20f * d) * 0.65f

    // Accueil
    playButton.set(cx - bw, h * 0.62f, cx + bw, h * 0.62f + 56f * d)
    settingsButton.set(cx - bw, h * 0.62f + 72f * d, cx + bw, h * 0.62f + 128f * d)

    // Fin de partie
    val ow = minOf(140f * d, w / 2f - 20f * d)
    overCard.set(cx - ow, cy - 115f * d, cx + ow, cy + 170f * d)
    replayButton.set(cx - bw, cy + 48f * d, cx + bw, cy + 92f * d)
    overMenuButton.set(cx - bw, cy + 112f * d, cx + bw, cy + 156f * d)

    // Panneau Pause / Paramètres
    panelTop = maxOf(40f * d, h / 2f - 245f * d)
    trackLeft = cx - hw + 24f * d
    trackRight = cx + hw - 24f * d
    modeTiltButton.set(trackLeft, panelTop + 104f * d, cx - 4f * d, panelTop + 140f * d)
    modeTouchButton.set(cx + 4f * d, panelTop + 104f * d, trackRight, panelTop + 140f * d)
    moveY = panelTop + 200f * d
    micY = panelTop + 270f * d
    previewY = panelTop + 298f * d
    defaultsButton.set(cx - 90f * d, panelTop + 315f * d, cx + 90f * d, panelTop + 347f * d)
    primaryButton.set(cx - bw, panelTop + 365f * d, cx + bw, panelTop + 409f * d)
    secondaryButton.set(cx - bw, panelTop + 425f * d, cx + bw, panelTop + 469f * d)
    pauseCard.set(cx - hw, panelTop, cx + hw, panelTop + 490f * d)
    settingsCard.set(cx - hw, panelTop, cx + hw, panelTop + 430f * d)

    // Barre de contrôle tactile, juste au-dessus de la jauge du micro
    controlY = h - 120f * d
}
