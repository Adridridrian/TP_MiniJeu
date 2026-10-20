package com.example.tp_minijeu

import kotlin.math.abs
import kotlin.random.Random

// ======================= OBSTACLES =======================
internal fun GameView.spawnObstacle() {
    val isTree = Random.nextBoolean()
    val size = if (isTree) (28f + Random.nextFloat() * 16f) * d
    else (22f + Random.nextFloat() * 16f) * d
    // N'importe où sur la largeur, y compris à cheval sur un bord
    val x = Random.nextFloat() * width
    obstacles.add(Obstacle(x, height + 3f * size, isTree, size))
}

/** L'obstacle touché par le bonhomme, ou null s'il n'en touche aucun. */
internal fun GameView.hitsObstacle(baseY: Float, s: Float): Obstacle? {
    val lean = playerLean()
    val margin = hitboxGrace * d
    // On teste aussi les copies du bonhomme à gauche/droite : couvre les obstacles à cheval sur un bord
    for (ox in floatArrayOf(0f, -width.toFloat(), width.toFloat())) {
        val px = posX + ox
        var body: List<HitCircle>? = null      // calculé seulement si un obstacle est proche
        for (o in obstacles) {
            // Tri rapide : obstacle trop loin du bonhomme (en hauteur ou en largeur)
            if (o.y - 2.5f * o.size > baseY + 90f * s || o.y + o.size < baseY - 160f * s) continue
            if (abs(o.x - px) > 1.5f * o.size + 70f * s) continue

            if (body == null) body = playerHitCircles(px, baseY, lean)
            for (shape in obstacleHitShapes(o, o.x)) {
                for (circle in body) if (circle.hits(shape, margin)) return o
            }
        }
    }
    return null
}
