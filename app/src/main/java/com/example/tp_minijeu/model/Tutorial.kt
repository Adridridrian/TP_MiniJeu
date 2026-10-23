package com.example.tp_minijeu.model

import com.example.tp_minijeu.controller.*

/**
 * Tutoriel interactif (ouvert avec le bouton "?" de l'accueil).
 * Étape 0 : choix du mode de contrôle (immersif ou tactile).
 * Étapes 1 à 4 : animations muettes qui montrent chaque interaction
 * (se diriger, éviter les obstacles, le micro, la pause). Dessin : view/TutorialScreen.kt
 */
internal const val TUTORIAL_LAST_STEP = 4

internal fun GameView.startTutorial() {
    gameState = GameState.TUTORIAL
    goToTutorialStep(0)
}

internal fun GameView.goToTutorialStep(step: Int) {
    tutorialStep = step
    tutorialTime = 0f               // chaque animation repart du début
}

/** Étape 0 : le mode choisi est enregistré comme dans les paramètres, puis on passe à la suite. */
internal fun GameView.chooseControl(touch: Boolean) {
    touchMode = touch
    saveSettings()
    goToTutorialStep(1)
}

/** Dernière étape : "Jouer !" lance une partie. */
internal fun GameView.finishTutorial() {
    resetGame()
    gameState = GameState.PLAYING
}

internal fun GameView.updateTutorial(dt: Float) {
    tutorialTime += dt
}
