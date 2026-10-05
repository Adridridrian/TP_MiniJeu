package com.example.tp_minijeu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

class GameView(context: Context) : View(context) {

    var tilt = 0f

    private val d = resources.displayMetrics.density   // 1dp en pixels
    private val taille = 0.6f                          // taille du bonhomme

    // ---------- Réglages du jeu ----------
    private val speedFactor = 75f * d     // sensibilité gauche/droite
    private val baseScroll = 220f * d     // vitesse de descente
    private val maxDifficulty = 1f        // 1f = vitesse constante
    private val secondsToMax = 45f

    // ---------- État du jeu ----------
    private var posX = -1f
    private var lastTime = 0L
    private var clock = 0f
    private var scrollSpeed = baseScroll
    private var gameTime = 0f
    private var distance = 0f
    private var best = 0
    private var gameOver = false
    private var gameOverAt = 0L
    private var spawnTimer = 1.5f
    private var sprayAcc = 0f

    private class Obstacle(var x: Float, var y: Float, val isTree: Boolean, val size: Float) {
        val shape = FloatArray(8) { 0.8f + Random.nextFloat() * 0.35f }   // forme du rocher
    }
    private class Particle(var x: Float, var y: Float, val vx: Float, val vy: Float, var life: Float)
    private class Flake(var x: Float, var y: Float, val r: Float, val speed: Float, val phase: Float)

    private val obstacles = mutableListOf<Obstacle>()
    private val particles = mutableListOf<Particle>()
    private val airFlakes = mutableListOf<Flake>()
    private val trail = ArrayDeque<PointF>()

    // ---------- Pinceaux ----------
    private fun fill(c: String) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(c); style = Paint.Style.FILL
    }
    private fun line(c: String, w: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(c); style = Paint.Style.STROKE
        strokeWidth = w; strokeCap = Paint.Cap.ROUND
    }
    private fun text(c: String, sizeDp: Float, align: Paint.Align) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor(c); textSize = sizeDp * d
        textAlign = align; isFakeBoldText = true
    }

    // Décor
    private val bg = Paint()
    private val shadow = fill("#2A2C4A6E")
    private val track = line("#B8CCE0", 3f * d)
    private val spray = fill("#BFD3E6")
    private val flakeFill = fill("#F0FFFFFF")
    private val flakeEdge = line("#4D8FA6BF", 0.8f * d)

    // Bonhomme
    private val ball = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ballEdge = line("#A9BED2", 1.5f * d)
    private val coal = fill("#263238")
    private val glint = fill("#FFFFFF")
    private val cheek = fill("#55FF8A80")
    private val carrot = fill("#FF8F00")
    private val carrotLine = line("#E65100", 1f * d)
    private val scarf = fill("#E53935")
    private val scarfDark = fill("#B71C1C")
    private val scarfStripe = fill("#FFEBEE")
    private val scarfLine = line("#E53935", 1.5f * d)
    private val ski = fill("#1565C0")
    private val skiShine = line("#90CAF9", 2f * d)
    private val binding = fill("#37474F")
    private val hat = fill("#212121")
    private val hatBand = fill("#D32F2F")
    private val hatShine = line("#5A5A5A", 1.5f * d)

    // Obstacles
    private val trunk = fill("#5D4037")
    private val treeGreen = fill("#2E7D32")
    private val treeDark = fill("#1B5E20")
    private val snowFlat = fill("#FFFFFF")
    private val rock = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rockEdge = line("#4A525C", 1.2f * d)

    // Interface
    private val pill = fill("#E6FFFFFF")
    private val pillShadow = fill("#22000000")
    private val scoreText = text("#1A3A5C", 20f, Paint.Align.LEFT)
    private val overlay = fill("#80102030")
    private val card = fill("#FFFFFF")
    private val cardShadow = fill("#33000000")
    private val titleText = text("#1A3A5C", 36f, Paint.Align.CENTER)
    private val infoText = text("#455A64", 19f, Paint.Align.CENTER)
    private val button = fill("#1E88E5")
    private val buttonText = text("#FFFFFF", 20f, Paint.Align.CENTER)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        bg.shader = LinearGradient(
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
    }

    private fun score() = (distance / (20f * d)).toInt()

    // ======================= BOUCLE DE JEU =======================
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)

        val now = System.nanoTime()
        val dt = if (lastTime == 0L) 0f
        else ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
        lastTime = now
        clock += dt

        if (posX < 0) posX = width / 2f
        val baseY = maxOf(height * 0.25f, 80f * d)
        val s = d * taille
        var velocity = 0f

        if (!gameOver) {
            gameTime += dt
            val difficulty = minOf(1f + gameTime / secondsToMax * (maxDifficulty - 1f), maxDifficulty)
            scrollSpeed = baseScroll * difficulty

            // Déplacement gauche/droite avec passage d'un bord à l'autre
            velocity = -tilt * speedFactor
            posX += velocity * dt
            if (width > 0) posX = ((posX % width) + width) % width

            val dy = scrollSpeed * dt
            distance += dy

            // Traces
            trail.forEach { it.y -= dy }
            while (trail.isNotEmpty() && trail.first().y < -20f) trail.removeFirst()
            trail.addLast(PointF(posX, baseY - 15f * s))

            // Gerbes de neige quand il tourne
            if (abs(velocity) > 60f * d) {
                sprayAcc += dt * 50f
                while (sprayAcc >= 1f) {
                    sprayAcc -= 1f
                    val dir = -sign(velocity)
                    particles.add(Particle(
                        posX + dir * 12f * s + (Random.nextFloat() - 0.5f) * 10f * s,
                        baseY + 40f * s,
                        dir * (40f + Random.nextFloat() * 100f) * d,
                        -Random.nextFloat() * 50f * d,
                        0.5f
                    ))
                }
            }

            // Obstacles
            obstacles.forEach { it.y -= dy }
            obstacles.removeAll { it.y < -150f * d }
            spawnTimer -= dt
            if (spawnTimer <= 0f) {
                spawnObstacle()
                spawnTimer = (0.5f + Random.nextFloat() * 0.7f) / difficulty
            }

            if (hitsObstacle(baseY, s)) {
                gameOver = true
                gameOverAt = System.currentTimeMillis()
                best = maxOf(best, score())
            }
        }

        // Particules
        val dyNow = if (gameOver) 0f else scrollSpeed * dt
        particles.forEach { it.x += it.vx * dt; it.y += it.vy * dt - dyNow; it.life -= dt }
        particles.removeAll { it.life <= 0f }

        // Neige qui tombe (continue même en fin de partie)
        for (f in airFlakes) {
            f.y += f.speed * dt
            f.x += sin(clock * 1.5f + f.phase) * 12f * d * dt
            if (f.y > height + 5f) { f.y = -5f; f.x = Random.nextFloat() * width }
        }

        // ---------- Dessin ----------
        drawTrack(canvas, -14f * s)
        drawTrack(canvas, 14f * s)

        obstacles.filter { it.y <= baseY + 55f * s }.forEach { drawObstacle(canvas, it) }

        for (p in particles) {
            spray.alpha = (p.life / 0.5f * 220).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, 2.2f * d, spray)
        }

        // Bonhomme (+ sa copie de l'autre côté quand il passe un bord)
        val lean = (velocity / speedFactor * 2.5f).coerceIn(-20f, 20f)
        for (ox in floatArrayOf(0f, -width.toFloat(), width.toFloat())) {
            val cx = posX + ox
            if (cx < -60f * d || cx > width + 60f * d) continue
            canvas.drawOval(cx - 22f * s, baseY + 5f * s, cx + 48f * s, baseY + 62f * s, shadow)
            canvas.save()
            canvas.rotate(lean, cx, baseY + 20f * s)
            canvas.scale(taille, taille, cx, baseY)
            drawSnowman(canvas, cx, baseY)
            canvas.restore()
        }

        obstacles.filter { it.y > baseY + 55f * s }.forEach { drawObstacle(canvas, it) }

        for (f in airFlakes) {
            canvas.drawCircle(f.x, f.y, f.r, flakeFill)
            canvas.drawCircle(f.x, f.y, f.r, flakeEdge)
        }

        drawHud(canvas)
        if (gameOver) drawGameOver(canvas)

        postInvalidateOnAnimation()
    }

    // ======================= OBSTACLES =======================
    private fun spawnObstacle() {
        val isTree = Random.nextBoolean()
        val size = if (isTree) (28f + Random.nextFloat() * 16f) * d
        else (22f + Random.nextFloat() * 16f) * d
        val x = size + Random.nextFloat() * maxOf(1f, width - 2 * size)
        obstacles.add(Obstacle(x, height + 3f * size, isTree, size))
    }

    private fun hitsObstacle(baseY: Float, s: Float): Boolean {
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

    private fun drawObstacle(c: Canvas, o: Obstacle) {
        if (o.isTree) drawTree(c, o.x, o.y, o.size) else drawRock(c, o)
    }

    private fun drawTree(c: Canvas, x: Float, y: Float, s: Float) {
        // Ombre
        c.drawOval(x - 0.6f * s, y - 0.25f * s, x + 1.4f * s, y + 0.25f * s, shadow)
        // Tronc
        c.drawRoundRect(RectF(x - 0.18f * s, y - 0.55f * s, x + 0.18f * s, y), 0.05f * s, 0.05f * s, trunk)

        // 3 étages : (bas, haut, demi-largeur)
        val tiers = listOf(
            Triple(y - 0.35f * s, y - 1.5f * s, s),
            Triple(y - 0.95f * s, y - 1.95f * s, 0.8f * s),
            Triple(y - 1.45f * s, y - 2.4f * s, 0.6f * s)
        )
        for ((b, t, w) in tiers) {
            // Feuillage
            val leaf = Path().apply {
                moveTo(x - w, b); quadTo(x, b + 0.18f * s, x + w, b); lineTo(x, t); close()
            }
            c.drawPath(leaf, treeGreen)
            // Côté à l'ombre
            val shade = Path().apply {
                moveTo(x, t); lineTo(x + w, b); quadTo(x + 0.5f * w, b + 0.12f * s, x, b + 0.09f * s); close()
            }
            c.drawPath(shade, treeDark)
            // Neige sur les branches
            val h = b - t
            val snowCap = Path().apply {
                moveTo(x, t)
                lineTo(x - w * 0.42f, t + h * 0.42f)
                quadTo(x - w * 0.2f, t + h * 0.3f, x - w * 0.05f, t + h * 0.5f)
                quadTo(x + w * 0.15f, t + h * 0.32f, x + w * 0.42f, t + h * 0.42f)
                close()
            }
            c.drawPath(snowCap, snowFlat)
        }
    }

    private fun drawRock(c: Canvas, o: Obstacle) {
        val x = o.x; val y = o.y; val r = o.size
        // Ombre
        c.drawOval(x - 0.8f * r, y + 0.1f * r, x + 1.3f * r, y + 0.55f * r, shadow)

        // Forme irrégulière
        val path = Path()
        for (i in 0 until 8) {
            val a = i * PI / 4
            val px = x + (cos(a) * r * o.shape[i]).toFloat()
            val py = y - 0.2f * r + (sin(a) * r * 0.65f * o.shape[i]).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()

        rock.shader = LinearGradient(
            x - r, y - r, x + r, y + 0.5f * r,
            Color.parseColor("#A3ACB6"), Color.parseColor("#545C66"),
            Shader.TileMode.CLAMP
        )
        c.drawPath(path, rock)
        c.drawPath(path, rockEdge)

        // Neige sur le dessus
        c.save()
        c.clipPath(path)
        c.drawOval(x - 0.9f * r, y - 1.1f * r, x + 0.7f * r, y - 0.55f * r, snowFlat)
        c.restore()
    }

    // ======================= INTERFACE =======================
    private fun drawHud(c: Canvas) {
        val label = "${score()} m"
        val tw = scoreText.measureText(label)
        val right = width - 16f * d
        val left = right - tw - 32f * d
        val top = 40f * d
        val bottom = 76f * d
        val rad = 18f * d
        c.drawRoundRect(RectF(left, top + 3f * d, right, bottom + 3f * d), rad, rad, pillShadow)
        c.drawRoundRect(RectF(left, top, right, bottom), rad, rad, pill)
        c.drawText(label, left + 16f * d, 65f * d, scoreText)
    }

    private fun drawGameOver(c: Canvas) {
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlay)
        val cx = width / 2f
        val cy = height * 0.45f
        val hw = minOf(140f * d, width / 2f - 20f * d)
        val rad = 24f * d

        c.drawRoundRect(RectF(cx - hw, cy - 111f * d, cx + hw, cy + 119f * d), rad, rad, cardShadow)
        c.drawRoundRect(RectF(cx - hw, cy - 115f * d, cx + hw, cy + 115f * d), rad, rad, card)

        c.drawText("Perdu !", cx, cy - 55f * d, titleText)
        c.drawText("Distance : ${score()} m", cx, cy - 10f * d, infoText)
        c.drawText("Record : $best m", cx, cy + 22f * d, infoText)

        c.drawRoundRect(RectF(cx - hw * 0.65f, cy + 48f * d, cx + hw * 0.65f, cy + 92f * d), 22f * d, 22f * d, button)
        c.drawText("Rejouer", cx, cy + 77f * d, buttonText)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && gameOver &&
            System.currentTimeMillis() - gameOverAt > 700) {
            performClick()
            restart()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun restart() {
        gameOver = false
        obstacles.clear()
        particles.clear()
        trail.clear()
        distance = 0f
        gameTime = 0f
        scrollSpeed = baseScroll
        spawnTimer = 1.5f
        posX = width / 2f
    }

    // ======================= BONHOMME DE NEIGE =======================
    private fun drawTrack(c: Canvas, offset: Float) {
        if (trail.size < 2) return
        val p = Path()
        var prevX = 0f
        trail.forEachIndexed { i, pt ->
            // On coupe la trace quand le bonhomme passe d'un bord à l'autre
            if (i == 0 || abs(pt.x - prevX) > width / 2f) p.moveTo(pt.x + offset, pt.y)
            else p.lineTo(pt.x + offset, pt.y)
            prevX = pt.x
        }
        c.drawPath(p, track)
    }

    private fun shadedBall(c: Canvas, cx: Float, cy: Float, r: Float) {
        ball.shader = RadialGradient(
            cx - 0.35f * r, cy - 0.4f * r, 1.4f * r,
            intArrayOf(Color.WHITE, Color.parseColor("#F1F6FA"), Color.parseColor("#C5D5E4")),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, r, ball)
        c.drawCircle(cx, cy, r, ballEdge)
    }

    private fun drawSnowman(c: Canvas, x: Float, y: Float) {
        // Skis
        for (side in intArrayOf(-1, 1)) {
            val sx = x + side * 14f * d
            c.drawRoundRect(RectF(sx - 5f * d, y - 15f * d, sx + 5f * d, y + 55f * d), 5f * d, 5f * d, ski)
            c.drawLine(sx - 1.5f * d, y - 10f * d, sx - 1.5f * d, y + 48f * d, skiShine)
            c.drawRect(sx - 5.5f * d, y + 8f * d, sx + 5.5f * d, y + 14f * d, binding)
        }

        // Corps
        shadedBall(c, x, y, 30f * d)
        shadedBall(c, x, y - 42f * d, 22f * d)

        // Boutons en charbon
        for (by in floatArrayOf(y - 48f * d, y - 38f * d)) {
            c.drawCircle(x, by, 2.6f * d, coal)
            c.drawCircle(x - 0.8f * d, by - 0.8f * d, 0.8f * d, glint)
        }

        // Tête
        shadedBall(c, x, y - 82f * d, 17f * d)

        // Joues
        c.drawCircle(x - 10f * d, y - 78f * d, 3.5f * d, cheek)
        c.drawCircle(x + 10f * d, y - 78f * d, 3.5f * d, cheek)

        // Yeux
        for (ex in floatArrayOf(x - 6f * d, x + 6f * d)) {
            c.drawCircle(ex, y - 87f * d, 2.6f * d, coal)
            c.drawCircle(ex - 0.8f * d, y - 87.8f * d, 0.8f * d, glint)
        }

        // Sourire
        for (i in -2..2) {
            val mx = x + i * 3.5f * d
            val my = y - 74f * d + (4 - i * i) * 0.5f * d
            c.drawCircle(mx, my, 1.2f * d, coal)
        }

        // Nez carotte
        val nose = Path().apply {
            moveTo(x, y - 83f * d); lineTo(x, y - 77f * d); lineTo(x + 14f * d, y - 79.5f * d); close()
        }
        c.drawPath(nose, carrot)
        c.drawLine(x + 4f * d, y - 81.5f * d, x + 4f * d, y - 78.5f * d, carrotLine)
        c.drawLine(x + 8f * d, y - 80.8f * d, x + 8f * d, y - 78.8f * d, carrotLine)

        // Écharpe
        c.drawRoundRect(RectF(x - 18f * d, y - 67f * d, x + 18f * d, y - 58f * d), 4f * d, 4f * d, scarf)
        c.drawRoundRect(RectF(x - 17f * d, y - 61f * d, x + 17f * d, y - 58.5f * d), 2f * d, 2f * d, scarfDark)
        c.drawRect(x - 9f * d, y - 67f * d, x - 6f * d, y - 58f * d, scarfStripe)
        c.drawRect(x - 1f * d, y - 67f * d, x + 2f * d, y - 58f * d, scarfStripe)
        // Pan de l'écharpe
        c.drawRoundRect(RectF(x + 5f * d, y - 62f * d, x + 13f * d, y - 43f * d), 3f * d, 3f * d, scarf)
        c.drawRect(x + 5f * d, y - 52f * d, x + 13f * d, y - 49f * d, scarfStripe)
        for (fx in floatArrayOf(6.5f, 9f, 11.5f)) {
            c.drawLine(x + fx * d, y - 43f * d, x + fx * d, y - 39f * d, scarfLine)
        }

        // Chapeau
        c.drawRoundRect(RectF(x - 12f * d, y - 120f * d, x + 12f * d, y - 98f * d), 2f * d, 2f * d, hat)
        c.drawRect(x - 12f * d, y - 104f * d, x + 12f * d, y - 100f * d, hatBand)
        c.drawRoundRect(RectF(x - 20f * d, y - 100f * d, x + 20f * d, y - 95f * d), 2.5f * d, 2.5f * d, hat)
        c.drawLine(x - 8f * d, y - 117f * d, x - 8f * d, y - 106f * d, hatShine)
    }
}