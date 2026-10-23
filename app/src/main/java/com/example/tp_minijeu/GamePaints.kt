package com.example.tp_minijeu

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
