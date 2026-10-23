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
    startLeaderboardButton.set(cx - bw, h * 0.62f + 72f * d, cx + bw, h * 0.62f + 128f * d)
    settingsButton.set(cx - bw, h * 0.62f + 144f * d, cx + bw, h * 0.62f + 200f * d)   // Paramètres en dernier
    // Difficulté : un sélecteur en 3 segments collés, au-dessus de "Jouer" (emoji + libellé)
    val dl = cx - hw + 24f * d
    val segW = 2f * (hw - 24f * d) / 3f
    difficultyButtons.forEachIndexed { i, r ->
        val l = dl + i * segW
        r.set(l, h * 0.62f - 66f * d, l + segW, h * 0.62f - 14f * d)
    }

    // Fin de partie
    val ow = minOf(140f * d, w / 2f - 20f * d)
    overCard.set(cx - ow, cy - 115f * d, cx + ow, cy + 236f * d)
    replayButton.set(cx - bw, cy + 48f * d, cx + bw, cy + 92f * d)
    overLeaderboardButton.set(cx - bw, cy + 112f * d, cx + bw, cy + 156f * d)
    overMenuButton.set(cx - bw, cy + 176f * d, cx + bw, cy + 220f * d)   // Menu en dernier

    // Panneau Pause / Paramètres
    panelTop = maxOf(40f * d, h / 2f - 266f * d)
    trackLeft = cx - hw + 24f * d
    trackRight = cx + hw - 24f * d

    // Classement : même carte que les paramètres. En-tête (titre + sous-titre) 104dp,
    // 10 lignes (44dp, réduites si l'écran est petit), puis le bouton "Retour" (20 + 44 + 20dp)
    rowHeight = minOf(44f * d, (h - 48f * d - 188f * d) / LEADERBOARD_ROWS)
    val boardHeight = 104f * d + LEADERBOARD_ROWS * rowHeight + 84f * d
    val boardTop = maxOf(24f * d, (h - boardHeight) / 2f)
    leaderboardCard.set(cx - hw, boardTop, cx + hw, boardTop + boardHeight)
    listTop = boardTop + 104f * d
    val listBottom = listTop + LEADERBOARD_ROWS * rowHeight
    backButton.set(cx - bw, listBottom + 14f * d, cx + bw, listBottom + 58f * d)
    modeTiltButton.set(trackLeft, panelTop + 104f * d, cx - 4f * d, panelTop + 140f * d)
    modeTouchButton.set(cx + 4f * d, panelTop + 104f * d, trackRight, panelTop + 140f * d)
    moveY = panelTop + 200f * d
    volumeY = panelTop + 270f * d
    previewY = panelTop + 338f * d
    defaultsButton.set(cx - 90f * d, panelTop + 357f * d, cx + 90f * d, panelTop + 389f * d)
    primaryButton.set(cx - bw, panelTop + 407f * d, cx + bw, panelTop + 451f * d)
    secondaryButton.set(cx - bw, panelTop + 467f * d, cx + bw, panelTop + 511f * d)
    pauseCard.set(cx - hw, panelTop, cx + hw, panelTop + 532f * d)
    settingsCard.set(cx - hw, panelTop, cx + hw, panelTop + 472f * d)

    // Barre de contrôle tactile, juste au-dessus de la jauge du micro
    controlY = h - 120f * d
}
