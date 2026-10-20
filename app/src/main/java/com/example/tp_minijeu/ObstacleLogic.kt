package com.example.tp_minijeu

import kotlin.math.hypot
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

internal fun GameView.hitsObstacle(baseY: Float, s: Float): Boolean {
    // On teste aussi les copies du bonhomme à gauche/droite : couvre les obstacles à cheval sur un bord
    for (ox in floatArrayOf(0f, -width.toFloat(), width.toFloat())) {
        val px = posX + ox
        val body = listOf(
            Triple(px, baseY, 30f * s),
            Triple(px, baseY - 42f * s, 22f * s),
            Triple(px, baseY + 35f * s, 14f * s)
        )
        for (o in obstacles) {
            val (cx, cy, r) = if (o.isTree) Triple(o.x, o.y - 1.1f * o.size, 0.75f * o.size)
            else Triple(o.x, o.y - 0.15f * o.size, 0.85f * o.size)
            for ((bx, by, br) in body) {
                if (hypot(cx - bx, cy - by) < r + br - 2f * d) return true
            }
        }
    }
    return false
}
