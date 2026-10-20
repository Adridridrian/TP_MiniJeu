package com.example.tp_minijeu

enum class GameState {
    START,
    PLAYING,
    PAUSED,      // pause en jeu = écran de paramètres avec "Reprendre" / "Menu"
    SETTINGS,    // paramètres ouverts depuis le menu de lancement
    LEADERBOARD, // tableau des scores
    GAME_OVER
}
