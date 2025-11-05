@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.example.juegoks_memorama.model
import kotlinx.serialization.Serializable

@Serializable // Nueva clase serializable para el historial
data class Move(
    val card1Id: Int,
    val card2Id: Int
)
@Serializable
data class Card(
    val id: Int,
    val value: Int,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)

enum class Difficulty(val pairs: Int, val columns: Int) {
    EASY(6, 4), // 4x3 grid = 12 cards, 6 pares
    MEDIUM(15, 5), // 6x5 grid = 30 cards, 15 pares (la cuadrícula actual era 5 columnas)
    HARD(28, 8) // 8x7 grid = 56 cards, 28 pares
}
enum class GameMode {
    SINGLE_PLAYER,
    BLUETOOTH
}

enum class SaveFormat {
    JSON,
    XML,
    TXT
}
@Serializable
data class GameState(
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val cards: List<Card> = emptyList(),
    val moves: Int = 0,
    val matchedPairs: Int = 0,
    val score: Int = 0,
    val matchStreak: Int = 0,
    val moveHistory: List<Move> = emptyList(),
    val gameCompleted: Boolean = false,
    val elapsedTimeInSeconds: Long = 0,
    val isTimerRunning: Boolean = false
)

data class GameHistoryItem(
    val filename: String,
    val format: SaveFormat,
    val state: GameState
)