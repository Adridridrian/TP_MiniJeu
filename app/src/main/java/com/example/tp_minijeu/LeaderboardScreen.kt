package com.example.tp_minijeu

import android.graphics.Canvas
import android.graphics.Path
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Créé une seule fois (pas à chaque image) : affiche la date d'une partie en "12/10"
private val dateFormat = SimpleDateFormat("dd/MM", Locale.FRANCE)

// Réutilisé pour chaque glaçon (on évite de créer un Path à chaque image)
private val iciclePath = Path()

// ======================= ÉCRAN DU CLASSEMENT =======================
internal fun GameView.drawLeaderboardPanel(c: Canvas) {
    val cx = width / 2f

    // Retour et titre TOUJOURS dessinés, même si la liste est vide :
    // sinon le joueur ne voit pas comment revenir en arrière
    drawBackButton(c)
    val titleY = backButton.centerY() - (paints.titleText.ascent() + paints.titleText.descent()) / 2f
    c.drawText("Classement", cx, titleY, paints.titleText)

    if (leaderboard.entries.isEmpty()) {
        drawEmptyLeaderboard(c)
        return
    }

    drawYourRank(c, lastRank)   // lastRank = propriété de GameView, remplie à la fin de la partie
    drawRankRows(c)
}

/** Premier lancement : aucune partie, on explique comment remplir le classement. */
internal fun GameView.drawEmptyLeaderboard(c: Canvas) {
    val cx = width / 2f
    val cy = height * 0.4f
    c.drawText("Aucune partie pour l'instant", cx, cy, paints.infoText)
    c.drawText("Lance une partie !", cx, cy + 30f * d, paints.infoText)
}

/** Bouton retour : rond blanc avec une ombre, et une flèche vers la gauche. */
internal fun GameView.drawBackButton(c: Canvas) {
    val bx = backButton.centerX()
    val by = backButton.centerY()
    val r = backButton.width() / 2f      // 24dp : le rond dessiné = toute la zone touchable (48dp)

    // 1. L'ombre (le même rond décalé de 3dp vers le bas), puis le rond par-dessus
    c.drawCircle(bx, by + 3f * d, r, paints.pillShadow)
    c.drawCircle(bx, by, r, paints.pill)

    // 2. La flèche : une tige horizontale + deux branches qui partent de la pointe
    val tipX = bx - 8f * d               // la pointe, à gauche du centre
    c.drawLine(bx + 8f * d, by, tipX, by, paints.backArrow)               // tige
    c.drawLine(tipX, by, tipX + 7f * d, by - 7f * d, paints.backArrow)   // branche du haut
    c.drawLine(tipX, by, tipX + 7f * d, by + 7f * d, paints.backArrow)   // branche du bas
}

/** Carte "Toi : X m — #N" au-dessus de la liste, si la dernière partie est dans le top 10. */
internal fun GameView.drawYourRank(c: Canvas, rank: Int?) {
    if (rank == null) return                       // pas de partie jouée, ou hors du top 10
    if (rank >= leaderboard.entries.size) return   // sécurité : ne jamais lire hors de la liste

    // Score lu dans le classement : score() vaut 0 si on est passé par "Menu" (resetGame)
    val myScore = leaderboard.entries[rank].first

    // Zone de la carte : entre le titre et la liste, même largeur que les lignes
    val hw = minOf(160f * d, width / 2f - 20f * d)
    val cx = width / 2f
    tmpRect.set(cx - hw, listTop - 68f * d, cx + hw, listTop - 16f * d)

    // 1. Le fond d'abord (ombre + carte), sinon il recouvrirait le texte
    val rad = tmpRect.height() / 2f
    c.drawRoundRect(tmpRect.left, tmpRect.top + 3f * d, tmpRect.right, tmpRect.bottom + 3f * d,
        rad, rad, paints.pillShadow)
    c.drawRoundRect(tmpRect, rad, rad, paints.buttonAlt)

    // 2. Le texte, centré verticalement (y = ligne de base, voir drawButton)
    val ty = tmpRect.centerY() - (paints.buttonAltText.ascent() + paints.buttonAltText.descent()) / 2f
    c.drawText("Toi : $myScore m — #${rank + 1}", cx, ty, paints.buttonAltText)   // rang 0 = 1re place
}

/** Les 10 meilleurs scores : un cadre, puis une rangée par score (rang + date à gauche, score à droite). */
internal fun GameView.drawRankRows(c: Canvas) {
    val entries = leaderboard.entries
    if (entries.isEmpty()) return            // rien à encadrer (message affiché par drawEmptyLeaderboard)

    val cx = width / 2f
    val hw = minOf(160f * d, width / 2f - 20f * d)   // demi-largeur des lignes (comme la carte "Toi")

    // 1. Les glaçons, puis le cadre autour de toutes les lignes (8dp plus large de chaque côté)
    //    bas du cadre = bas de la dernière ligne (listTop + n * rowHeight - 8dp) + 8dp de marge
    val frameBottom = listTop + entries.size * rowHeight
    drawIcicles(c, frameBottom)              // avant le cadre : le cadre recouvre le haut des glaçons
    tmpRect.set(cx - hw - 8f * d, listTop - 8f * d, cx + hw + 8f * d, frameBottom)
    drawCard(c, tmpRect)

    for ((i, entry) in entries.withIndex()) {
        val (score, date) = entry            // Pair(score, date) : le 2e élément est la date, pas un nom
        val top = listTop + i * rowHeight

        // 2. Fond de la ligne (8dp d'espace avec la suivante) — la dernière partie en bleu clair
        tmpRect.set(cx - hw, top, cx + hw, top + rowHeight - 8f * d)
        val rad = tmpRect.height() / 2f
        c.drawRoundRect(tmpRect, rad, rad, if (i == lastRank) paints.buttonAlt else paints.rowFill)

        // 3. Les textes APRÈS le fond, centrés verticalement dans la ligne
        //    labelText est aligné à gauche et valueText à droite : x = bord du texte, pas son centre
        val ty = tmpRect.centerY() - (paints.labelText.ascent() + paints.labelText.descent()) / 2f
        c.drawText("#${i + 1}   ${dateFormat.format(Date(date))}", tmpRect.left + 16f * d, ty, paints.labelText)
        c.drawText("$score m", tmpRect.right - 16f * d, ty, paints.valueText)
    }
}

/** Glaçons qui pendent sous le cadre. bottom = bas du cadre. Formes tirées dans setupLayout. */
internal fun GameView.drawIcicles(c: Canvas, bottom: Float) {
    val top = bottom - 6f * d                // la base commence sous le cadre (cachée par lui)
    for (ic in icicles) {
        // Un triangle pointe vers le bas : base gauche -> base droite -> pointe
        iciclePath.reset()
        iciclePath.moveTo(ic.x - ic.halfWidth, top)
        iciclePath.lineTo(ic.x + ic.halfWidth, top)
        iciclePath.lineTo(ic.x, bottom + ic.length)
        iciclePath.close()
        c.drawPath(iciclePath, paints.ice)       // remplissage
        c.drawPath(iciclePath, paints.iceEdge)   // contour

        // Petit reflet blanc sur le côté gauche : donne un effet de glace brillante
        c.drawLine(ic.x - ic.halfWidth * 0.35f, bottom + 2f * d,
            ic.x - ic.halfWidth * 0.1f, bottom + ic.length * 0.55f, paints.iceGlint)
    }
}
