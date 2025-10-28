package com.example.juegoks_memorama.model

data class Card(
    val id: Int,
    val value: Int,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)

data class GameState(
    val cards: List<Card> = emptyList(),
    val moves: Int = 0,
    val matchedPairs: Int = 0,
    val gameCompleted: Boolean = false
)