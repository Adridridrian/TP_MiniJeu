package com.example.tp_minijeu.view

import com.example.tp_minijeu.controller.*
import com.example.tp_minijeu.model.*
import android.graphics.Canvas
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Créé une seule fois (pas à chaque image) : affiche la date d'une partie en "12/10"
private val dateFormat = SimpleDateFormat("dd/MM", Locale.FRANCE)

// Nombre de lignes du classement (le top 10, places vides comprises)
internal const val LEADERBOARD_ROWS = 10

// ======================= ÉCRAN DU CLASSEMENT =======================
// Même présentation que le panneau Paramètres : carte blanche centrée, titre en haut,
// contenu aux mêmes marges que les curseurs, bouton "Retour" en bas.
internal fun GameView.drawLeaderboardPanel(c: Canvas) {
    drawCard(c, leaderboardCard)
    val cx = width / 2f
    val top = leaderboardCard.top
    c.drawText("Classement", cx, top + 50f * d, paints.titleText)

    // Sous-titre : place de la dernière partie, ou comment remplir le classement
    val rank = lastRank
    val entries = leaderboard.entries
    when {
        entries.isEmpty() -> c.drawText("Joue une partie pour y entrer !", cx, top + 82f * d, paints.subtitleText)
        rank != null && rank < entries.size ->
            c.drawText("Dernière partie : ${entries[rank].first} m, ${rank + 1}e place", cx, top + 82f * d, paints.subtitleText)
    }

    drawRankRows(c)
    drawButton(c, backButton, "Retour", paints.button, paints.buttonText)
}

/** Les 10 places : rang + date à gauche, score à droite. Places vides en gris, dernière partie en bleu. */
internal fun GameView.drawRankRows(c: Canvas) {
    val entries = leaderboard.entries
    for (i in 0 until LEADERBOARD_ROWS) {
        val rowTop = listTop + i * rowHeight
        tmpRect.set(trackLeft, rowTop, trackRight, rowTop + rowHeight - 6f * d)
        val rad = tmpRect.height() / 2f
        c.drawRoundRect(tmpRect, rad, rad, if (i == lastRank) paints.buttonAlt else paints.rowFill)

        // Textes centrés verticalement (y = ligne de base, voir drawButton)
        val ty = tmpRect.centerY() - (paints.labelText.ascent() + paints.labelText.descent()) / 2f
        val left = tmpRect.left + 16f * d
        val right = tmpRect.right - 16f * d
        if (i < entries.size) {
            val (score, date) = entries[i]           // Pair(score, date)
            c.drawText("#${i + 1}   ${dateFormat.format(Date(date))}", left, ty, paints.labelText)
            c.drawText("$score m", right, ty, paints.valueText)
        } else {
            c.drawText("#${i + 1}", left, ty, paints.rowEmptyText)
            c.drawText("—", right, ty, paints.rowEmptyValue)
        }
    }
}
