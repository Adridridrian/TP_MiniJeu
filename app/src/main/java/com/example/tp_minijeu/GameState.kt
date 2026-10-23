package com.example.tp_minijeu

enum class GameState {
    START,
    PLAYING,
    PAUSED,      // pause en jeu = écran de paramètres avec "Reprendre" / "Menu"
    SETTINGS,    // paramètres ouverts depuis le menu de lancement
    LEADERBOARD, // tableau des scores
    CRASHING,    // dernière vie perdue : le bonhomme se démonte avant l'écran "Perdu !"
    GAME_OVER
}
