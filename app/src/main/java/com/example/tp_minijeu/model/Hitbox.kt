package com.example.tp_minijeu.model

import com.example.tp_minijeu.controller.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Cercle de collision (centre et rayon en pixels). */
internal class HitCircle(val x: Float, val y: Float, val r: Float)

/** Polygone de collision (sommets en pixels, dans l'ordre). */
internal class HitPolygon(val xs: FloatArray, val ys: FloatArray)

// ======================= BONHOMME =======================
// Seule la partie posée au sol compte (skis + boule du bas) : le haut du corps
// peut passer devant / derrière un obstacle sans le toucher (effet de profondeur).
// Cercles en dp, origine = centre de la boule du bas, avant la mise à l'échelle par "taille"
private val snowmanCircles = arrayOf(
    floatArrayOf(0f, 0f, 30f),                                       // boule du bas
    // Avant des skis (l'arrière est caché sous la boule du bas)
    floatArrayOf(-14f, 30f, 5f), floatArrayOf(-14f, 40f, 5f), floatArrayOf(-14f, 50f, 5f),
    floatArrayOf(14f, 30f, 5f), floatArrayOf(14f, 40f, 5f), floatArrayOf(14f, 50f, 5f)
)

/** Inclinaison du bonhomme (degrés) selon sa vitesse : la même au dessin et aux collisions. */
internal fun GameView.playerLean() = (velocity / speedFactor * 2.5f).coerceIn(-20f, 20f)

/** Cercles du bonhomme dessiné en (cx, y) et penché de lean degrés (même transformation que drawPlayer). */
internal fun GameView.playerHitCircles(cx: Float, y: Float, lean: Float): List<HitCircle> {
    val a = (lean * PI / 180.0).toFloat()
    val cosA = cos(a)
    val sinA = sin(a)
    val pivotY = y + 20f * s
    return snowmanCircles.map { (dx, dy, r) ->
        // Mise à l'échelle autour de (cx, y), puis rotation autour du pivot
        val px = dx * s
        val py = y + dy * s - pivotY
        HitCircle(cx + px * cosA - py * sinA, pivotY + px * sinA + py * cosA, r * s)
    }
}

// ======================= OBSTACLES =======================
/**
 * Formes de collision de l'obstacle dessiné en x : seulement sa base au sol.
 * Rocher : sa forme entière (il est bas). Sapin : le tronc et le bas des branches,
 * le reste du feuillage peut recouvrir le bonhomme sans le toucher.
 */
internal fun obstacleHitShapes(o: Obstacle, x: Float): List<HitPolygon> {
    if (!o.isTree) return listOf(rockPoints(o, x))

    val y = o.y; val s = o.size
    // Tronc
    val trunk = HitPolygon(
        floatArrayOf(x - 0.18f * s, x + 0.18f * s, x + 0.18f * s, x - 0.18f * s),
        floatArrayOf(y - 0.55f * s, y - 0.55f * s, y, y)
    )
    // Bas de l'étage inférieur : de sa base (arrondie vers le bas) jusqu'à baseHeight au-dessus
    val (b, t, w) = treeTiers(y, s)[0]
    val baseHeight = 0.4f * s
    val topW = w * (1f - baseHeight / (b - t))      // demi-largeur du triangle à cette hauteur
    val skirt = HitPolygon(
        floatArrayOf(x - w, x, x + w, x + topW, x - topW),
        floatArrayOf(b, b + 0.09f * s, b, b - baseHeight, b - baseHeight)
    )
    return listOf(trunk, skirt)
}

// ======================= TESTS =======================
/** Le cercle touche-t-il le polygone ? margin = tolérance en pixels (le cercle est un peu réduit). */
internal fun HitCircle.hits(p: HitPolygon, margin: Float): Boolean {
    val rr = r - margin
    if (rr <= 0f) return false
    var inside = false
    val n = p.xs.size
    var j = n - 1
    for (i in 0 until n) {
        val xi = p.xs[i]; val yi = p.ys[i]
        val xj = p.xs[j]; val yj = p.ys[j]
        // Le bord du cercle coupe un côté du polygone
        if (distToSegment(x, y, xi, yi, xj, yj) < rr) return true
        // Centre à l'intérieur du polygone (lancer de rayon horizontal)
        if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
        j = i
    }
    return inside
}

private fun distToSegment(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
    val dx = bx - ax
    val dy = by - ay
    val len2 = dx * dx + dy * dy
    val t = if (len2 == 0f) 0f else (((px - ax) * dx + (py - ay) * dy) / len2).coerceIn(0f, 1f)
    return hypot(px - (ax + t * dx), py - (ay + t * dy))
}
