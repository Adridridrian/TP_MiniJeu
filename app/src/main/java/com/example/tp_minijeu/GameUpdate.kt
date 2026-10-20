package com.example.tp_minijeu

import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

internal fun GameView.score() = (distance / (20f * d)).toInt()

internal fun GameView.resetGame() {
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
internal fun GameView.update() {
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

internal fun GameView.updatePlaying(dt: Float) {
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
