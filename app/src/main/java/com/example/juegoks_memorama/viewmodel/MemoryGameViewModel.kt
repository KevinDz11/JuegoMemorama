package com.example.juegoks_memorama.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juegoks_memorama.data.GameRepository
import com.example.juegoks_memorama.model.Card //import com.example.memorygame.model.Card
import com.example.juegoks_memorama.model.GameMode // --- CAMBIO: Importar GameMode
import com.example.juegoks_memorama.model.GameState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update // <--- AÑADIR IMPORT
import kotlinx.coroutines.isActive // <--- AÑADIR IMPORT
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryGameViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState = _gameState.asStateFlow()

    private var firstSelectedCard: Card? = null
    private var secondSelectedCard: Card? = null
    private var canFlip = true
    private var timerJob: Job? = null
    private var currentGameMode: GameMode = GameMode.SINGLE_PLAYER // --- CAMBIO: Seguir el modo actual

    init {
        viewModelScope.launch {
            // Intenta cargar la partida guardada
            val savedGame = repository.savedGameState.firstOrNull()
            if (savedGame != null && !savedGame.gameCompleted) {
                _gameState.value = savedGame
                // Asumimos que el juego guardado es siempre Un Jugador.
                currentGameMode = GameMode.SINGLE_PLAYER
                if (savedGame.isTimerRunning) {
                    startTimer() // Reanuda el timer si estaba corriendo
                }
            } else {
                // Se espera que setGameMode sea llamado al iniciar la pantalla
            }

            // Lanza el observador para guardar automáticamente
            observeAndSaveGameState()
        }
    }

    // --- CAMBIO: Nuevo método para establecer el modo de juego ---
    fun setGameMode(mode: GameMode) {
        if (currentGameMode != mode) {
            currentGameMode = mode
            startNewGame()

            if (mode == GameMode.BLUETOOTH) {
                // Lógica de inicialización de Bluetooth (por implementar)
            } else {
                // Lógica para detener Bluetooth si se vuelve a Single Player
            }
        } else if (_gameState.value.cards.isEmpty()) {
            // Iniciar juego si no hay cartas (primera carga)
            startNewGame()
        }
    }
    // -------------------------------------------------------------

    private fun observeAndSaveGameState() {
        viewModelScope.launch {
            gameState.collect { state ->
                // --- CAMBIO: Solo guardar en modo Un Jugador ---
                if (currentGameMode == GameMode.SINGLE_PLAYER && !state.gameCompleted && state.moves > 0) {
                    repository.saveGameState(state)
                }
                if(state.gameCompleted){
                    repository.clearGameState() // Limpia al completar
                }
            }
        }
    }

    fun startNewGame() {
        timerJob?.cancel()
        val cardValues = (1..15).flatMap { listOf(it, it) }.shuffled()
        val cards = cardValues.mapIndexed { index, value ->
            Card(id = index, value = value)
        }

        _gameState.value = GameState(cards = cards)
        firstSelectedCard = null
        secondSelectedCard = null
        canFlip = true

        viewModelScope.launch {
            repository.clearGameState()
        }

        if (currentGameMode == GameMode.BLUETOOTH) {
            // Lógica de inicio de partida Bluetooth (por implementar)
        }
    }

    private fun startTimer() {
        if (currentGameMode == GameMode.BLUETOOTH) return // --- CAMBIO: No usar el timer local en Bluetooth

        if (_gameState.value.isTimerRunning) return // No iniciar si ya está corriendo

        _gameState.update { it.copy(isTimerRunning = true) }
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _gameState.update {
                    it.copy(elapsedTimeInSeconds = it.elapsedTimeInSeconds + 1)
                }
            }
        }
    }

    fun onCardClick(card: Card) {
        // --- CAMBIO: Lógica inicial para Bluetooth ---
        if (currentGameMode == GameMode.BLUETOOTH) {
            // Antes de continuar, aquí se debería enviar el movimiento por Bluetooth y
            // esperar a que el otro jugador lo valide/reciba.
            // Para la demo, continuaremos con la lógica local por ahora.
        }
        // ------------------------------------------

        if (!canFlip || card.isFaceUp || card.isMatched) return

        startTimer()

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
            val gameCompleted = newMatchedPairs == 15

            // --- MODIFICAR ESTO ---
            if (gameCompleted) {
                timerJob?.cancel() // Detiene el timer
                if (currentGameMode == GameMode.BLUETOOTH) {
                    // Lógica para declarar al ganador y terminar la conexión
                }
            }

            _gameState.value = currentState.copy(
                cards = updatedCards,
                matchedPairs = newMatchedPairs,
                gameCompleted = gameCompleted,
                isTimerRunning = !gameCompleted // Actualiza el estado del timer
            )
            // --- FIN DE MODIFICACIÓN ---

            if (currentGameMode == GameMode.BLUETOOTH) {
                // Lógica de "turno extra" o "pasar turno" para Bluetooth
            }
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

            if (currentGameMode == GameMode.BLUETOOTH) {
                // Lógica de "pasar turno"
            }
        }

        firstSelectedCard = null
        secondSelectedCard = null
        canFlip = true
    }
}