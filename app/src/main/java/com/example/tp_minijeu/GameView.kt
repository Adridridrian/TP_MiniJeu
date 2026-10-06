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
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

enum class GameState {
    START,
    PLAYING,
    PAUSED,      // pause en jeu = écran de paramètres avec "Reprendre" / "Menu"
    SETTINGS,    // paramètres ouverts depuis le menu de lancement
    GAME_OVER
}

class GameView(context: Context) : View(context) {

    // Inclinaison filtrée, mise à jour par MainActivity (accéléromètre)
    var tilt = 0f

    // Niveau sonore brut du micro entre 0 et 1, mis à jour par MainActivity (thread du micro)
    @Volatile var micLevel = 0f
    // false si l'autorisation du micro est refusée ou si le micro est indisponible
    @Volatile var micAvailable = false

    private val d = resources.displayMetrics.density   // 1dp en pixels
    private val taille = 0.6f                          // taille du bonhomme
    private val s = d * taille                         // échelle du bonhomme en pixels

    // ---------- Réglages du jeu ----------
    private val speedFactor = 75f * d     // sensibilité gauche/droite de base (= 100 %)
    private val deadZone = 0.6f           // inclinaison ignorée (m/s²) : le téléphone n'est jamais parfaitement à plat
    private val baseScroll = 220f * d     // vitesse de descente
    private val maxDifficulty = 1f        // 1f = vitesse constante
    private val secondsToMax = 45f

    // ---------- Réglages du micro ----------
    private val micThreshold = 0.7f       // position de la barre (0..1) : au-dessus, la vitesse augmente
    private val speedStep = 0.1f          // +10 % de vitesse à chaque dépassement
    private val speedCooldownTime = 1f    // délai mini (s) entre deux augmentations
    private val maxSpeed = 3f             // vitesse maximum (x3)

    private val micAttack = 8f            // vitesse de montée de la jauge (plus haut = plus réactif)
    private val micRelease = 2.5f         // vitesse de descente de la jauge (plus bas = plus doux)

    // ---------- Paramètres modifiables par le joueur (sauvegardés) ----------
    private val moveMin = 0.25f; private val moveMax = 2f     // sensibilité mouvement : 25 % .. 200 %
    private val micMin = 0.5f;   private val micMax = 2f      // sensibilité micro : 50 % .. 200 %
    private val prefs = context.getSharedPreferences("reglages", Context.MODE_PRIVATE)
    private var moveSens = prefs.getFloat("moveSens", 1f)
    private var micSens = prefs.getFloat("micSens", 1f)

    private fun saveSettings() {
        prefs.edit().putFloat("moveSens", moveSens).putFloat("micSens", micSens).apply()
    }

    // ---------- État du jeu ----------
    private var gameState = GameState.START

    private var posX = -1f
    private var baseY = 0f                // hauteur fixe du bonhomme (calculée dans onSizeChanged)
    private var velocity = 0f
    private var lastTime = 0L
    private var clock = 0f
    private var scrollSpeed = baseScroll
    private var gameTime = 0f
    private var distance = 0f
    private var best = 0
    private var gameOverAt = 0L
    private var spawnTimer = 1.5f
    private var sprayAcc = 0f
    private var micDisplay = 0f           // niveau du micro après sensibilité (affiché par la jauge)
    private var speedMultiplier = 1f      // vitesse actuelle (ne redescend jamais pendant une partie)
    private var speedCooldown = 0f
    private var speedFlash = 0f           // > 0 : la vitesse vient d'augmenter (affichage en rouge)
    private var dragging = 0              // curseur en cours de glissement : 0 aucun, 1 mouvement, 2 micro

    private class Obstacle(var x: Float, var y: Float, val isTree: Boolean, val size: Float) {
        val shape = FloatArray(8) { 0.8f + Random.nextFloat() * 0.35f }   // forme du rocher
    }
    private class Particle(var x: Float, var y: Float, val vx: Float, val vy: Float, var life: Float)
    private class Flake(var x: Float, var y: Float, val r: Float, val speed: Float, val phase: Float)

    private val obstacles = mutableListOf<Obstacle>()
    private val particles = mutableListOf<Particle>()
    private val airFlakes = mutableListOf<Flake>()
    private val trail = ArrayDeque<PointF>()

    // ---------- Zones des boutons et du panneau (calculées dans onSizeChanged) ----------
    private val playButton = RectF()
    private val settingsButton = RectF()
    private val replayButton = RectF()
    private val overMenuButton = RectF()
    private val overCard = RectF()

    private val pauseCard = RectF()       // panneau en jeu (2 boutons)
    private val settingsCard = RectF()    // panneau depuis le menu (1 bouton)
    private val primaryButton = RectF()   // "Reprendre" ou "Retour"
    private val secondaryButton = RectF() // "Menu" (seulement en jeu)
    private val defaultsButton = RectF()  // "Valeurs par défaut"
    private var panelTop = 0f
    private var trackLeft = 0f
    private var trackRight = 0f
    private var moveY = 0f                // hauteur du curseur mouvement
    private var micY = 0f                 // hauteur du curseur micro
    private var previewY = 0f             // hauteur de la jauge de test du micro

    private val tmpRect = RectF()         // réutilisé pour éviter d'allouer pendant le dessin

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
    private val hudIcon = fill("#1A3A5C")
    private val scoreText = text("#1A3A5C", 20f, Paint.Align.LEFT)
    private val overlay = fill("#80102030")
    private val card = fill("#FFFFFF")
    private val cardShadow = fill("#33000000")
    private val titleText = text("#1A3A5C", 36f, Paint.Align.CENTER)
    private val infoText = text("#455A64", 19f, Paint.Align.CENTER)
    private val button = fill("#1E88E5")
    private val buttonText = text("#FFFFFF", 20f, Paint.Align.CENTER)
    private val buttonAlt = fill("#E3F2FD")
    private val buttonAltText = text("#1E88E5", 20f, Paint.Align.CENTER)
    private val hintText = text("#1A3A5C", 15f, Paint.Align.CENTER)

    // Paramètres
    private val labelText = text("#1A3A5C", 16f, Paint.Align.LEFT)
    private val valueText = text("#1E88E5", 16f, Paint.Align.RIGHT)
    private val linkText = text("#1E88E5", 15f, Paint.Align.CENTER)
    private val sliderFill = fill("#1E88E5")
    private val thumbFill = fill("#FFFFFF")
    private val thumbEdge = line("#1E88E5", 3f * d)

    // Jauge du micro
    private val gaugeTrack = fill("#CFDCE8")
    private val gaugeCalm = fill("#43A047")
    private val gaugeLoud = fill("#E53935")
    private val gaugeMark = line("#1A3A5C", 2f * d)
    private val micBody = line("#1A3A5C", 2f * d)
    private val speedText = text("#1A3A5C", 17f, Paint.Align.RIGHT)
    private val speedUpText = text("#E53935", 19f, Paint.Align.RIGHT)
    private val micOffText = text("#78909C", 14f, Paint.Align.LEFT)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

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

    private fun score() = (distance / (20f * d)).toInt()

    // ======================= BOUCLE DE JEU =======================
    private val gameLoop: Runnable = object : Runnable {
        override fun run() {
            update()
            invalidate()             // demande un nouveau dessin
            postDelayed(this, 16)    // environ 60 mises à jour/s
        }
    }

    fun startGame() {
        lastTime = 0L                // évite un saut de dt au retour d'arrière-plan
        removeCallbacks(gameLoop)
        post(gameLoop)
    }

    fun stopGame() {
        removeCallbacks(gameLoop)
        // L'appli passe en arrière-plan (appel, notification...) : on ne fait pas perdre la partie
        if (gameState == GameState.PLAYING) gameState = GameState.PAUSED
        dragging = 0
        invalidate()
    }

    private fun resetGame() {
        obstacles.clear()
        particles.clear()
        trail.clear()
        distance = 0f
        gameTime = 0f
        scrollSpeed = baseScroll
        spawnTimer = 1.5f
        sprayAcc = 0f
        speedMultiplier = 1f
        speedCooldown = 0f
        speedFlash = 0f
        velocity = 0f
        posX = width / 2f
    }

    // ======================= MISE À JOUR =======================
    private fun update() {
        // Temps réellement écoulé : même vitesse de jeu quel que soit le téléphone
        val now = System.nanoTime()
        val dt = if (lastTime == 0L) 0f
        else ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
        lastTime = now

        // Niveau du micro après sensibilité : c'est cette valeur que la jauge affiche
        val micTarget = (micLevel * micSens).coerceIn(0f, 1f)
        val k = if (micTarget > micDisplay) micAttack else micRelease
        micDisplay += (micTarget - micDisplay) * minOf(1f, dt * k)

        if (gameState == GameState.PAUSED) return   // tout est figé (la jauge de test reste active)
        clock += dt
        if (speedFlash > 0f) speedFlash -= dt

        if (gameState == GameState.PLAYING) updatePlaying(dt)

        // Gerbes de neige (finissent de retomber après la chute)
        val dyNow = if (gameState == GameState.PLAYING) scrollSpeed * dt else 0f
        particles.forEach { it.x += it.vx * dt; it.y += it.vy * dt - dyNow; it.life -= dt }
        particles.removeAll { it.life <= 0f }

        // Neige qui tombe : décor présent sur tous les écrans
        for (f in airFlakes) {
            f.y += f.speed * dt
            f.x += sin(clock * 1.5f + f.phase) * 12f * d * dt
            if (f.y > height + 5f) { f.y = -5f; f.x = Random.nextFloat() * width }
        }
    }

    private fun updatePlaying(dt: Float) {
        gameTime += dt
        val difficulty = minOf(1f + gameTime / secondsToMax * (maxDifficulty - 1f), maxDifficulty)

        // Son au-dessus de la barre : la vitesse augmente définitivement (jusqu'à la fin de la partie)
        if (speedCooldown > 0f) speedCooldown -= dt
        if (micAvailable && micDisplay > micThreshold && speedCooldown <= 0f && speedMultiplier < maxSpeed) {
            speedMultiplier = minOf(speedMultiplier + speedStep, maxSpeed)
            speedCooldown = speedCooldownTime
            speedFlash = 0.6f
        }
        scrollSpeed = baseScroll * difficulty * speedMultiplier

        // Zone morte : petites inclinaisons ignorées, puis la vitesse repart de 0 au-delà du seuil
        val effectiveTilt = if (abs(tilt) < deadZone) 0f else tilt - sign(tilt) * deadZone

        // Déplacement gauche/droite (avec la sensibilité choisie) et passage d'un bord à l'autre
        velocity = -effectiveTilt * speedFactor * moveSens
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
            spawnTimer = (0.5f + Random.nextFloat() * 0.7f) / (difficulty * speedMultiplier)
        }

        if (hitsObstacle(baseY, s)) {
            gameState = GameState.GAME_OVER
            gameOverAt = System.currentTimeMillis()
            best = maxOf(best, score())
            velocity = 0f
        }
    }

    // ======================= DESSIN DES ÉCRANS =======================
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (gameState) {
            GameState.START -> drawStartScreen(canvas)
            GameState.PLAYING -> drawGame(canvas)
            GameState.PAUSED -> drawPauseScreen(canvas)
            GameState.SETTINGS -> drawSettingsScreen(canvas)
            GameState.GAME_OVER -> drawGameOverScreen(canvas)
        }
    }

    private fun drawStartScreen(c: Canvas) {
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
        val cx = width / 2f
        c.drawText("MINI-JEU", cx, height * 0.18f, titleText)
        c.drawText("Record : $best m", cx, height * 0.18f + 36f * d, infoText)
        drawPlayer(c, cx, height * 0.42f, 0f)
        drawButton(c, playButton, "Jouer", button, buttonText)
        drawButton(c, settingsButton, "Paramètres", buttonAlt, buttonAltText)
        drawFallingSnow(c)
    }

    private fun drawGame(c: Canvas) {
        drawScene(c)
        drawHud(c)

        // Rappel en début de partie : toucher l'écran = pause / paramètres
        if (gameTime < 3f) {
            hintText.alpha = ((3f - gameTime).coerceIn(0f, 1f) * 255).toInt()
            c.drawText("Touche l'écran : pause / paramètres", width / 2f, height - 100f * d, hintText)
        }
    }

    private fun drawPauseScreen(c: Canvas) {
        drawScene(c)
        drawHud(c)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlay)
        drawSettingsPanel(c, "Pause", pauseCard, inGame = true)
    }

    private fun drawSettingsScreen(c: Canvas) {
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
        drawFallingSnow(c)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlay)
        drawSettingsPanel(c, "Paramètres", settingsCard, inGame = false)
    }

    private fun drawGameOverScreen(c: Canvas) {
        drawScene(c)
        drawHud(c)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlay)
        drawCard(c, overCard)
        val cx = width / 2f
        val cy = height * 0.45f
        c.drawText("Perdu !", cx, cy - 55f * d, titleText)
        c.drawText("Distance : ${score()} m", cx, cy - 10f * d, infoText)
        c.drawText("Record : $best m", cx, cy + 22f * d, infoText)
        drawButton(c, replayButton, "Rejouer", button, buttonText)
        drawButton(c, overMenuButton, "Menu", buttonAlt, buttonAltText)
    }

    /** La piste : traces, obstacles, gerbes, bonhomme et neige qui tombe. */
    private fun drawScene(c: Canvas) {
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)

        drawTrack(c, -14f * s)
        drawTrack(c, 14f * s)

        // Obstacles derrière le bonhomme, puis devant (effet de profondeur)
        val limit = baseY + 55f * s
        for (o in obstacles) if (o.y <= limit) drawObstacleWrapped(c, o)

        for (p in particles) {
            spray.alpha = (p.life / 0.5f * 220).toInt().coerceIn(0, 255)
            c.drawCircle(p.x, p.y, 2.2f * d, spray)
        }

        // Bonhomme (+ sa copie de l'autre côté quand il passe un bord)
        val lean = (velocity / speedFactor * 2.5f).coerceIn(-20f, 20f)
        for (ox in floatArrayOf(0f, -width.toFloat(), width.toFloat())) {
            val cx = posX + ox
            if (cx < -60f * d || cx > width + 60f * d) continue
            drawPlayer(c, cx, baseY, lean)
        }

        for (o in obstacles) if (o.y > limit) drawObstacleWrapped(c, o)

        drawFallingSnow(c)
    }

    private fun drawPlayer(c: Canvas, cx: Float, y: Float, lean: Float) {
        c.drawOval(cx - 22f * s, y + 5f * s, cx + 48f * s, y + 62f * s, shadow)
        c.save()
        c.rotate(lean, cx, y + 20f * s)
        c.scale(taille, taille, cx, y)
        drawSnowman(c, cx, y)
        c.restore()
    }

    private fun drawFallingSnow(c: Canvas) {
        for (f in airFlakes) {
            c.drawCircle(f.x, f.y, f.r, flakeFill)
            c.drawCircle(f.x, f.y, f.r, flakeEdge)
        }
    }

    // ======================= PANNEAU PAUSE / PARAMÈTRES =======================
    private fun drawSettingsPanel(c: Canvas, title: String, cardRect: RectF, inGame: Boolean) {
        drawCard(c, cardRect)
        val cx = width / 2f
        c.drawText(title, cx, panelTop + 50f * d, titleText)

        // Sensibilité du mouvement
        c.drawText("Sensibilité mouvement", trackLeft, moveY - 22f * d, labelText)
        c.drawText(percent(moveSens), trackRight, moveY - 22f * d, valueText)
        drawSlider(c, moveY, (moveSens - moveMin) / (moveMax - moveMin))

        // Sensibilité du micro + jauge de test en direct
        c.drawText("Sensibilité micro", trackLeft, micY - 22f * d, labelText)
        c.drawText(percent(micSens), trackRight, micY - 22f * d, valueText)
        drawSlider(c, micY, (micSens - micMin) / (micMax - micMin))
        if (micAvailable) {
            drawLevelBar(c, trackLeft, trackRight, previewY, 4f * d)
        } else {
            c.drawText("Micro non autorisé", trackLeft, previewY + 5f * d, micOffText)
        }

        // Valeurs par défaut
        val ty = defaultsButton.centerY() - (linkText.ascent() + linkText.descent()) / 2f
        c.drawText("Valeurs par défaut", cx, ty, linkText)

        if (inGame) {
            drawButton(c, primaryButton, "Reprendre", button, buttonText)
            drawButton(c, secondaryButton, "Menu", buttonAlt, buttonAltText)
        } else {
            drawButton(c, primaryButton, "Retour", button, buttonText)
        }
    }

    private fun percent(v: Float) = "${(v * 100).roundToInt()} %"

    /** Curseur : barre, partie remplie et poignée ronde. t entre 0 et 1. */
    private fun drawSlider(c: Canvas, y: Float, t: Float) {
        val tt = t.coerceIn(0f, 1f)
        val h = 3f * d
        tmpRect.set(trackLeft, y - h, trackRight, y + h)
        c.drawRoundRect(tmpRect, h, h, gaugeTrack)
        val tx = trackLeft + (trackRight - trackLeft) * tt
        tmpRect.set(trackLeft, y - h, tx, y + h)
        c.drawRoundRect(tmpRect, h, h, sliderFill)
        c.drawCircle(tx, y + 2f * d, 12f * d, pillShadow)
        c.drawCircle(tx, y, 12f * d, thumbFill)
        c.drawCircle(tx, y, 12f * d, thumbEdge)
    }

    private fun hitSlider(sliderY: Float, x: Float, y: Float) =
        abs(y - sliderY) < 24f * d && x > trackLeft - 24f * d && x < trackRight + 24f * d

    private fun setSliderFromX(which: Int, x: Float) {
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

    // ======================= INTERFACE EN JEU =======================
    private fun drawHud(c: Canvas) {
        // Score en haut à gauche
        val label = "${score()} m"
        val tw = scoreText.measureText(label)
        val left = 16f * d
        val right = left + tw + 32f * d
        val top = 40f * d
        val bottom = 76f * d
        val rad = 18f * d
        tmpRect.set(left, top + 3f * d, right, bottom + 3f * d)
        c.drawRoundRect(tmpRect, rad, rad, pillShadow)
        tmpRect.set(left, top, right, bottom)
        c.drawRoundRect(tmpRect, rad, rad, pill)
        c.drawText(label, left + 16f * d, 65f * d, scoreText)

        drawMicGauge(c)
    }

    /** Jauge du micro en bas de l'écran : volume, barre de seuil et vitesse actuelle. */
    private fun drawMicGauge(c: Canvas) {
        val left = 16f * d
        val right = width - 16f * d
        val bottom = height - 32f * d
        val top = bottom - 44f * d
        val rad = 22f * d
        tmpRect.set(left, top + 3f * d, right, bottom + 3f * d)
        c.drawRoundRect(tmpRect, rad, rad, pillShadow)
        tmpRect.set(left, top, right, bottom)
        c.drawRoundRect(tmpRect, rad, rad, pill)

        // Icône micro
        val mx = left + 24f * d
        val my = (top + bottom) / 2f
        tmpRect.set(mx - 4f * d, my - 11f * d, mx + 4f * d, my + 3f * d)
        c.drawRoundRect(tmpRect, 4f * d, 4f * d, hudIcon)
        tmpRect.set(mx - 8f * d, my - 6f * d, mx + 8f * d, my + 7f * d)
        c.drawArc(tmpRect, 0f, 180f, false, micBody)
        c.drawLine(mx, my + 7f * d, mx, my + 11f * d, micBody)

        // Vitesse actuelle, à droite (en rouge juste après une accélération)
        val speedLabel = "x%.1f".format(speedMultiplier)
        val sp = if (speedFlash > 0f) speedUpText else speedText
        c.drawText(speedLabel, right - 16f * d, my + 6f * d, sp)

        val barLeft = left + 44f * d
        val barRight = right - 76f * d
        if (!micAvailable) {
            c.drawText("Micro non autorisé", barLeft, my + 5f * d, micOffText)
            return
        }
        drawLevelBar(c, barLeft, barRight, my, 6f * d)
    }

    /** Barre de niveau du micro avec repère du seuil (utilisée en jeu et dans les paramètres). */
    private fun drawLevelBar(c: Canvas, l: Float, r: Float, cy: Float, halfH: Float) {
        tmpRect.set(l, cy - halfH, r, cy + halfH)
        c.drawRoundRect(tmpRect, halfH, halfH, gaugeTrack)
        val level = micDisplay.coerceIn(0f, 1f)
        if (level > 0.01f) {
            tmpRect.set(l, cy - halfH, l + (r - l) * level, cy + halfH)
            c.drawRoundRect(tmpRect, halfH, halfH, if (micDisplay > micThreshold) gaugeLoud else gaugeCalm)
        }
        val sx = l + (r - l) * micThreshold
        c.drawLine(sx, cy - halfH - 5f * d, sx, cy + halfH + 5f * d, gaugeMark)
    }

    private fun drawCard(c: Canvas, r: RectF) {
        val rad = 24f * d
        tmpRect.set(r.left, r.top + 4f * d, r.right, r.bottom + 4f * d)
        c.drawRoundRect(tmpRect, rad, rad, cardShadow)
        c.drawRoundRect(r, rad, rad, card)
    }

    private fun drawButton(c: Canvas, r: RectF, label: String, fill: Paint, textPaint: Paint) {
        val rad = r.height() / 2f
        c.drawRoundRect(r, rad, rad, fill)
        // Centrage vertical : y de drawText = ligne de base, on la décale de la moitié de la hauteur du texte
        val ty = r.centerY() - (textPaint.ascent() + textPaint.descent()) / 2f
        c.drawText(label, r.centerX(), ty, textPaint)
    }

    // ======================= INTERACTIONS =======================
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> onTouchDown(x, y)
            MotionEvent.ACTION_MOVE -> if (dragging != 0) setSliderFromX(dragging, x)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging != 0) {
                    dragging = 0
                    saveSettings()
                }
            }
        }
        invalidate()
        return true
    }

    private fun onTouchDown(x: Float, y: Float) {
        when (gameState) {
            GameState.START -> {
                if (playButton.contains(x, y)) {
                    performClick()
                    resetGame()
                    gameState = GameState.PLAYING
                } else if (settingsButton.contains(x, y)) {
                    performClick()
                    gameState = GameState.SETTINGS
                }
            }
            GameState.PLAYING -> {
                // Toucher l'écran n'importe où = pause / paramètres
                performClick()
                gameState = GameState.PAUSED
            }
            GameState.PAUSED, GameState.SETTINGS -> {
                val inGame = gameState == GameState.PAUSED
                when {
                    hitSlider(moveY, x, y) -> { dragging = 1; setSliderFromX(1, x) }
                    hitSlider(micY, x, y) -> { dragging = 2; setSliderFromX(2, x) }
                    defaultsButton.contains(x, y) -> {
                        performClick()
                        moveSens = 1f
                        micSens = 1f
                        saveSettings()
                    }
                    primaryButton.contains(x, y) -> {
                        performClick()
                        gameState = if (inGame) GameState.PLAYING else GameState.START
                    }
                    inGame && secondaryButton.contains(x, y) -> {
                        performClick()
                        resetGame()
                        gameState = GameState.START
                    }
                }
            }
            GameState.GAME_OVER -> {
                // Délai de 700 ms : évite de relancer par erreur en touchant l'écran au moment de la chute
                if (System.currentTimeMillis() - gameOverAt > 700) {
                    if (replayButton.contains(x, y)) {
                        performClick()
                        resetGame()
                        gameState = GameState.PLAYING
                    } else if (overMenuButton.contains(x, y)) {
                        performClick()
                        resetGame()
                        gameState = GameState.START
                    }
                }
            }
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    // ======================= OBSTACLES =======================
    private fun spawnObstacle() {
        val isTree = Random.nextBoolean()
        val size = if (isTree) (28f + Random.nextFloat() * 16f) * d
        else (22f + Random.nextFloat() * 16f) * d
        // N'importe où sur la largeur, y compris à cheval sur un bord
        val x = Random.nextFloat() * width
        obstacles.add(Obstacle(x, height + 3f * size, isTree, size))
    }

    private fun hitsObstacle(baseY: Float, s: Float): Boolean {
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

    /** Dessine l'obstacle, et sa copie de l'autre côté de l'écran s'il dépasse d'un bord. */
    private fun drawObstacleWrapped(c: Canvas, o: Obstacle) {
        val w = width.toFloat()
        for (ox in floatArrayOf(0f, -w, w)) {
            val x = o.x + ox
            if (x < -2f * o.size || x > w + 2f * o.size) continue
            if (o.isTree) drawTree(c, x, o.y, o.size) else drawRock(c, o, x)
        }
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

    private fun drawRock(c: Canvas, o: Obstacle, x: Float) {
        val y = o.y; val r = o.size
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