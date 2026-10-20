package com.example.tp_minijeu

import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/**
 * Vue du jeu : contient l'état et les méthodes Android à surcharger.
 * Le reste est réparti dans des fichiers séparés (fonctions d'extension sur GameView) :
 *  - GameLayout.kt        : calcul des zones à l'écran
 *  - GameUpdate.kt        : mise à jour du jeu à chaque image
 *  - ObstacleLogic.kt     : apparition des obstacles et collisions
 *  - ScreenRenderer.kt    : dessin des différents écrans
 *  - SceneRenderer.kt     : dessin de la piste
 *  - SnowmanRenderer.kt   : dessin du bonhomme de neige
 *  - ObstacleRenderer.kt  : dessin des sapins et rochers
 *  - HudRenderer.kt       : score et jauge du micro
 *  - SettingsPanel.kt     : panneau pause / paramètres
 *  - UiComponents.kt      : cartes et boutons
 *  - TouchHandler.kt      : interactions tactiles
 */
class GameView(context: Context) : View(context) {

    // Inclinaison filtrée, mise à jour par MainActivity (accéléromètre)
    var tilt = 0f

    // Niveau sonore brut du micro entre 0 et 1, mis à jour par MainActivity (thread du micro)
    @Volatile var micLevel = 0f
    // false si l'autorisation du micro est refusée ou si le micro est indisponible
    @Volatile var micAvailable = false

    internal val d = resources.displayMetrics.density   // 1dp en pixels
    internal val taille = 0.6f                          // taille du bonhomme
    internal val s = d * taille                         // échelle du bonhomme en pixels

    // ---------- Réglages du jeu (dépendant de la densité, les autres sont dans GameConfig.kt) ----------
    internal val speedFactor = 75f * d     // sensibilité gauche/droite de base (= 100 %)
    internal val baseScroll = 220f * d     // vitesse de descente

    // ---------- Paramètres modifiables par le joueur (sauvegardés) ----------
    internal val prefs = context.getSharedPreferences("reglages", Context.MODE_PRIVATE)
    internal var moveSens = prefs.getFloat("moveSens", 1f)
    internal var micSens = prefs.getFloat("micSens", 1f)

    // ---------- État du jeu ----------
    internal var gameState = GameState.START

    internal var posX = -1f
    internal var baseY = 0f                // hauteur fixe du bonhomme (calculée dans onSizeChanged)
    internal var velocity = 0f
    internal var lastTime = 0L
    internal var clock = 0f
    internal var scrollSpeed = baseScroll
    internal var gameTime = 0f
    internal var distance = 0f
    internal var best = 0
    internal var gameOverAt = 0L
    internal var spawnTimer = 1.5f
    internal var sprayAcc = 0f
    internal var micDisplay = 0f           // niveau du micro après sensibilité (affiché par la jauge)
    internal var speedMultiplier = 1f      // vitesse actuelle (ne redescend jamais pendant une partie)
    internal var speedCooldown = 0f
    internal var speedFlash = 0f           // > 0 : la vitesse vient d'augmenter (affichage en rouge)
    internal var dragging = 0              // curseur en cours de glissement : 0 aucun, 1 mouvement, 2 micro

    internal val obstacles = mutableListOf<Obstacle>()
    internal val particles = mutableListOf<Particle>()
    internal val airFlakes = mutableListOf<Flake>()
    internal val trail = ArrayDeque<PointF>()

    // ---------- Zones des boutons et du panneau (calculées dans onSizeChanged) ----------
    internal val playButton = RectF()
    internal val settingsButton = RectF()
    internal val replayButton = RectF()
    internal val overMenuButton = RectF()
    internal val overCard = RectF()

    internal val pauseCard = RectF()       // panneau en jeu (2 boutons)
    internal val settingsCard = RectF()    // panneau depuis le menu (1 bouton)
    internal val primaryButton = RectF()   // "Reprendre" ou "Retour"
    internal val secondaryButton = RectF() // "Menu" (seulement en jeu)
    internal val defaultsButton = RectF()  // "Valeurs par défaut"
    internal var panelTop = 0f
    internal var trackLeft = 0f
    internal var trackRight = 0f
    internal var moveY = 0f                // hauteur du curseur mouvement
    internal var micY = 0f                 // hauteur du curseur micro
    internal var previewY = 0f             // hauteur de la jauge de test du micro

    internal val tmpRect = RectF()         // réutilisé pour éviter d'allouer pendant le dessin

    // ---------- Pinceaux ----------
    internal val paints = GamePaints(d)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        setupLayout(w, h)
    }

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

    // ======================= DESSIN =======================
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCurrentScreen(canvas)
    }

    // ======================= INTERACTIONS =======================
    override fun onTouchEvent(event: MotionEvent): Boolean = handleTouch(event)

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
