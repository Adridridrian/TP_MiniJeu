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
    startLeaderboardButton.set(cx - bw, h * 0.62f + 144f * d, cx + bw, h * 0.62f + 200f * d)

    // Fin de partie
    val ow = minOf(140f * d, w / 2f - 20f * d)
    overCard.set(cx - ow, cy - 115f * d, cx + ow, cy + 236f * d)
    replayButton.set(cx - bw, cy + 48f * d, cx + bw, cy + 92f * d)
    overMenuButton.set(cx - bw, cy + 112f * d, cx + bw, cy + 156f * d)
    overLeaderboardButton.set(cx - bw, cy + 176f * d, cx + bw, cy + 220f * d)

    // Classement : retour en haut à gauche (48dp), carte "Toi" en dessous, puis les 10 lignes
    backButton.set(16f * d, 40f * d, 64f * d, 88f * d)
    listTop = 172f * d
    // 52dp par ligne, réduit si l'écran est trop petit pour afficher les 10 lignes
    rowHeight = minOf(52f * d, (h - listTop - 24f * d) / 10f)

    // Glaçons sous le cadre du classement. Tirés au hasard UNE SEULE FOIS ici :
    // si on les tirait dans la fonction de dessin, ils changeraient à chaque image et clignoteraient.
    // On les place de gauche à droite, en évitant les coins arrondis du cadre (rayon 24dp).
    icicles.clear()
    val frameLeft = cx - hw - 8f * d
    val frameRight = cx + hw + 8f * d
    var ix = frameLeft + 26f * d
    while (ix < frameRight - 26f * d) {
        val halfWidth = (3f + Random.nextFloat() * 3f) * d       // base de 6 à 12dp
        val length = (8f + Random.nextFloat() * 12f) * d          // longueur de 8 à 20dp
        icicles.add(Icicle(ix, halfWidth, length))
        ix += 2f * halfWidth + (3f + Random.nextFloat() * 9f) * d // espace irrégulier avant le suivant
    }

    // Panneau Pause / Paramètres
    panelTop = maxOf(40f * d, h / 2f - 205f * d)
    trackLeft = cx - hw + 24f * d
    trackRight = cx + hw - 24f * d
    moveY = panelTop + 120f * d
    micY = panelTop + 190f * d
    previewY = panelTop + 218f * d
    defaultsButton.set(cx - 90f * d, panelTop + 235f * d, cx + 90f * d, panelTop + 267f * d)
    primaryButton.set(cx - bw, panelTop + 285f * d, cx + bw, panelTop + 329f * d)
    secondaryButton.set(cx - bw, panelTop + 345f * d, cx + bw, panelTop + 389f * d)
    pauseCard.set(cx - hw, panelTop, cx + hw, panelTop + 410f * d)
    settingsCard.set(cx - hw, panelTop, cx + hw, panelTop + 350f * d)
}
