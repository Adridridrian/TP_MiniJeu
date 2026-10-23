package com.example.tp_minijeu.model

enum class GameState {
    START,
    PLAYING,
    PAUSED,      // pause en jeu = écran de paramètres avec "Reprendre" / "Menu"
    SETTINGS,    // paramètres ouverts depuis le menu de lancement
    LEADERBOARD, // tableau des scores
    TUTORIAL,    // tutoriel interactif ouvert depuis l'accueil
    CRASHING,    // dernière vie perdue : le bonhomme se démonte avant l'écran "Perdu !"
    GAME_OVER
}
