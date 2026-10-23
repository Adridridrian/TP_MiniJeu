package com.example.tp_minijeu.view

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/** Tous les pinceaux du jeu. d = 1dp en pixels. */
internal class GamePaints(private val d: Float) {

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
    val bg = Paint()
    val shadow = fill("#2A2C4A6E")
    val track = line("#B8CCE0", 3f * d)
    val spray = fill("#BFD3E6")
    val flakeFill = fill("#F0FFFFFF")
    val flakeEdge = line("#4D8FA6BF", 0.8f * d)

    // Bonhomme
    val ball = Paint(Paint.ANTI_ALIAS_FLAG)
    val ballEdge = line("#A9BED2", 1.5f * d)
    val coal = fill("#263238")
    val glint = fill("#FFFFFF")
    val cheek = fill("#55FF8A80")
    val carrot = fill("#FF8F00")
    val carrotLine = line("#E65100", 1f * d)
    val scarf = fill("#E53935")
    val scarfDark = fill("#B71C1C")
    val scarfStripe = fill("#FFEBEE")
    val scarfLine = line("#E53935", 1.5f * d)
    val ski = fill("#1565C0")
    val skiShine = line("#90CAF9", 2f * d)
    val binding = fill("#37474F")
    val hat = fill("#212121")
    val hatBand = fill("#D32F2F")
    val hatShine = line("#5A5A5A", 1.5f * d)

    // Père Noël et son traîneau (skin secret)
    val santaRed = fill("#D32F2F")
    val santaRedDark = fill("#B71C1C")
    val santaBelly = Paint(Paint.ANTI_ALIAS_FLAG)        // dégradé posé au moment du dessin
    val fur = fill("#FAFAFA")
    val furEdge = line("#D7DCE0", 1f * d)
    val skin = fill("#FFCCBC")
    val santaNose = fill("#E57373")
    val belt = fill("#212121")
    val gold = fill("#FFCA28")
    val goldLine = line("#FFB300", 2f * d)
    val runner = fill("#FFB300")
    val runnerShine = line("#FFE082", 1.2f * d)
    val runnerCurl = line("#FFB300", 3f * d)
    val sleighBack = fill("#8E1B1B")
    val sleighFront = fill("#C62828")
    val sleighShade = fill("#9A1B1B")
    val mitten = fill("#1B5E20")
    val giftColors = arrayOf(fill("#2E7D32"), fill("#1565C0"))
    val ribbonColors = arrayOf(fill("#FFD54F"), fill("#E53935"))
    val magicSparkle = fill("#FFD54F")                  // étoiles dorées de la transformation

    // Tutoriel
    val tutoCaption = text("#455A64", 16f, Paint.Align.CENTER)    // les 2 lignes d'explication
    val tutoSmall = text("#455A64", 13f, Paint.Align.CENTER)      // description sous les choix
    val tutoQuestion = text("#1E88E5", 24f, Paint.Align.CENTER)   // "?" du bouton de l'accueil
    val tutoLabel = text("#1A3A5C", 12f, Paint.Align.CENTER)      // "Tuto" sous le bouton
    val tutoArrow = text("#1E88E5", 22f, Paint.Align.CENTER)      // flèches gauche / droite
    val tutoEmoji = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 44f * d; textAlign = Paint.Align.CENTER }
    val tutoEmojiSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 30f * d; textAlign = Paint.Align.CENTER }
    val tutoEmojiTiny = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 22f * d; textAlign = Paint.Align.CENTER }
    val tutoSelected = line("#1E88E5", 3f * d)                    // contour du mode choisi
    val tutoStageEdge = line("#CFDCE8", 1.5f * d)                 // contour de la zone d'animation
    val tutoRipple = line("#1E88E5", 3f * d)                      // cercle qui s'agrandit au toucher
    val tutoWave = line("#1A3A5C", 2.5f * d)                      // ondes sonores du micro
    val tutoDotOn = fill("#1E88E5")                               // point de l'étape en cours

    // Obstacles
    val trunk = fill("#5D4037")
    val treeGreen = fill("#2E7D32")
    val treeDark = fill("#1B5E20")
    val snowFlat = fill("#FFFFFF")
    val rock = Paint(Paint.ANTI_ALIAS_FLAG)
    val rockEdge = line("#4A525C", 1.2f * d)
    val hitbox = line("#E53935", 1.5f * d)

    // Interface
    val pill = fill("#E6FFFFFF")
    val pillShadow = fill("#22000000")
    val hudIcon = fill("#1A3A5C")
    val heartFull = fill("#E53935")
    val heartEmpty = fill("#CFD8DC")
    val scoreText = text("#1A3A5C", 20f, Paint.Align.LEFT)
    val overlay = fill("#80102030")
    val card = fill("#FFFFFF")
    val cardShadow = fill("#33000000")
    val titleText = text("#1A3A5C", 36f, Paint.Align.CENTER)
    val infoText = text("#455A64", 19f, Paint.Align.CENTER)
    val button = fill("#1E88E5")
    val buttonText = text("#FFFFFF", 20f, Paint.Align.CENTER)
    val buttonAlt = fill("#E3F2FD")
    val buttonAltText = text("#1E88E5", 20f, Paint.Align.CENTER)
    val hintText = text("#1A3A5C", 15f, Paint.Align.CENTER)
    val recordText = text("#E65100", 21f, Paint.Align.CENTER)   // "Nouveau record !"
    val rankText = text("#1E88E5", 19f, Paint.Align.CENTER)     // "4e place au classement"
    val rowFill = fill("#F2F4F7")                     // fond d'une ligne du classement (sur carte blanche)
    val rowEmptyText = text("#A0ACB8", 16f, Paint.Align.LEFT)    // place vide : "#7"
    val rowEmptyValue = text("#A0ACB8", 16f, Paint.Align.RIGHT)  // place vide : "—"
    val subtitleText = text("#1E88E5", 15f, Paint.Align.CENTER)  // sous-titre du classement

    // Titre "Jingle Rush" en bloc de glace (police Roboto Black intégrée à Android)
    val iceFill = text("#FFFFFF", 54f, Paint.Align.CENTER).apply {
        typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
        isFakeBoldText = false
        letterSpacing = 0.04f
    }
    val iceOutline = Paint(iceFill).apply {
        style = Paint.Style.STROKE; strokeWidth = 5f * d
        strokeJoin = Paint.Join.ROUND; color = Color.parseColor("#0D3B66")
    }
    val iceDepth = Paint(iceOutline).apply {
        style = Paint.Style.FILL_AND_STROKE; color = Color.parseColor("#0A2C4D")
    }
    val icicleFill = fill("#BFE6FF")
    val icicleEdge = line("#0D3B66", 2f * d)
    val sparkle = fill("#FFFFFF")

    // Paramètres
    val labelText = text("#1A3A5C", 16f, Paint.Align.LEFT)
    val valueText = text("#1E88E5", 16f, Paint.Align.RIGHT)
    val linkText = text("#1E88E5", 15f, Paint.Align.CENTER)
    val sliderFill = fill("#1E88E5")
    val thumbFill = fill("#FFFFFF")
    val thumbEdge = line("#1E88E5", 3f * d)
    // Choix de la difficulté : couleur du segment choisi (vert facile, orange moyen, rouge difficile)
    val difficultyFills = arrayOf(fill("#43A047"), fill("#FB8C00"), fill("#E53935"))
    val difficultyOnText = text("#FFFFFF", 13f, Paint.Align.CENTER)
    val difficultyOffText = text("#1A3A5C", 13f, Paint.Align.CENTER)
    val difficultyEmoji = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f * d; textAlign = Paint.Align.CENTER }
    val segmentOnText = text("#FFFFFF", 16f, Paint.Align.CENTER)
    val segmentOffText = text("#1E88E5", 16f, Paint.Align.CENTER)

    // Jauge du micro
    val gaugeTrack = fill("#CFDCE8")
    val gaugeCalm = fill("#43A047")
    val gaugeLoud = fill("#E53935")
    val gaugeMark = line("#1A3A5C", 2f * d)
    val micBody = line("#1A3A5C", 2f * d)
    val speedText = text("#1A3A5C", 17f, Paint.Align.RIGHT)
    val speedUpText = text("#E53935", 19f, Paint.Align.RIGHT)
    val micOffText = text("#78909C", 14f, Paint.Align.LEFT)
}
