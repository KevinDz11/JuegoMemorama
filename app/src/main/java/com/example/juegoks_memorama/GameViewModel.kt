package com.example.juegoks_memorama

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        resetGame()
    }

    fun resetGame() {
        val gameCards = (CardValues.icons + CardValues.icons) // Duplica la lista de iconos
            .shuffled() // Mezcla las cartas
            .mapIndexed { index, icon -> // Crea los objetos MemoryCard
                MemoryCard(
                    id = index,
                    imageVector = icon
                )
            }
        _uiState.value = GameUiState(cards = gameCards)
    }

    // Esta es la función principal que se llama al tocar una carta
    fun onCardSelected(cardIndex: Int) {
        // No hacer nada si la carta ya está volteada o si ya estamos revisando un par
        if (_uiState.value.isCheckingMatch || _uiState.value.cards[cardIndex].isFaceUp) {
            return
        }

        val selectedCards = _uiState.value.selectedCards
        val currentCards = _uiState.value.cards.toMutableList()

        // Voltea la carta seleccionada
        currentCards[cardIndex] = currentCards[cardIndex].copy(isFaceUp = true)

        // Actualiza el estado con la carta volteada
        _uiState.update {
            it.copy(
                cards = currentCards,
                selectedCards = selectedCards + cardIndex // Agrega el índice a la lista de seleccionadas
            )
        }

        // Si ya hay 2 cartas seleccionadas, revisa si son un par
        if (_uiState.value.selectedCards.size == 2) {
            checkMatch()
        }
    }

    private fun checkMatch() {
        // Pone el juego en modo "revisando" para que no se pueda hacer clic
        _uiState.update { it.copy(isCheckingMatch = true) }

        viewModelScope.launch {
            val (index1, index2) = _uiState.value.selectedCards
            val card1 = _uiState.value.cards[index1]
            val card2 = _uiState.value.cards[index2]

            // Espera 1 segundo para que el jugador vea la segunda carta
            delay(1000)

            val currentCards = _uiState.value.cards.toMutableList()

            if (card1.imageVector == card2.imageVector) {
                // ¡ES UN PAR!
                currentCards[index1] = card1.copy(isMatched = true)
                currentCards[index2] = card2.copy(isMatched = true)
                _uiState.update {
                    it.copy(
                        cards = currentCards,
                        score = it.score + 1, // Suma puntos
                        selectedCards = emptyList(), // Limpia las seleccionadas
                        isCheckingMatch = false // Termina de revisar
                    )
                }
            } else {
                // NO ES UN PAR
                currentCards[index1] = card1.copy(isFaceUp = false)
                currentCards[index2] = card2.copy(isFaceUp = false)
                _uiState.update {
                    it.copy(
                        cards = currentCards,
                        selectedCards = emptyList(), // Limpia las seleccionadas
                        isCheckingMatch = false // Termina de revisar
                    )
                }
            }
        }
    }
}