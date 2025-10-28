package com.example.juegoks_memorama.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juegoks_memorama.model.Card //import com.example.memorygame.model.Card
import com.example.juegoks_memorama.model.GameState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryGameViewModel @Inject constructor() : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState = _gameState.asStateFlow()

    private var firstSelectedCard: Card? = null
    private var secondSelectedCard: Card? = null
    private var canFlip = true

    init {
        startNewGame()
    }

    fun startNewGame() {
        val cardValues = (1..8).flatMap { listOf(it, it) }.shuffled()
        val cards = cardValues.mapIndexed { index, value ->
            Card(id = index, value = value)
        }

        _gameState.value = GameState(cards = cards)
        firstSelectedCard = null
        secondSelectedCard = null
        canFlip = true
    }

    fun onCardClick(card: Card) {
        if (!canFlip || card.isFaceUp || card.isMatched) return

        val currentState = _gameState.value
        val updatedCards = currentState.cards.toMutableList()
        val cardIndex = updatedCards.indexOfFirst { it.id == card.id }

        if (cardIndex == -1) return

        updatedCards[cardIndex] = card.copy(isFaceUp = true)

        if (firstSelectedCard == null) {
            firstSelectedCard = updatedCards[cardIndex]
            _gameState.value = currentState.copy(cards = updatedCards)
        } else {
            secondSelectedCard = updatedCards[cardIndex]
            canFlip = false

            _gameState.value = currentState.copy(
                cards = updatedCards,
                moves = currentState.moves + 1
            )

            viewModelScope.launch {
                delay(1000)
                checkForMatch()
            }
        }
    }

    private fun checkForMatch() {
        val currentState = _gameState.value
        val updatedCards = currentState.cards.toMutableList()

        val firstCard = firstSelectedCard
        val secondCard = secondSelectedCard

        if (firstCard != null && secondCard != null && firstCard.value == secondCard.value) {
            val firstIndex = updatedCards.indexOfFirst { it.id == firstCard.id }
            val secondIndex = updatedCards.indexOfFirst { it.id == secondCard.id }

            updatedCards[firstIndex] = firstCard.copy(isMatched = true)
            updatedCards[secondIndex] = secondCard.copy(isMatched = true)

            val newMatchedPairs = currentState.matchedPairs + 1
            val gameCompleted = newMatchedPairs == 8

            _gameState.value = currentState.copy(
                cards = updatedCards,
                matchedPairs = newMatchedPairs,
                gameCompleted = gameCompleted
            )
        } else {
            val firstIndex = updatedCards.indexOfFirst { it.id == firstCard?.id }
            val secondIndex = updatedCards.indexOfFirst { it.id == secondCard?.id }

            if (firstIndex != -1) {
                updatedCards[firstIndex] = updatedCards[firstIndex].copy(isFaceUp = false)
            }
            if (secondIndex != -1) {
                updatedCards[secondIndex] = updatedCards[secondIndex].copy(isFaceUp = false)
            }

            _gameState.value = currentState.copy(cards = updatedCards)
        }

        firstSelectedCard = null
        secondSelectedCard = null
        canFlip = true
    }
}