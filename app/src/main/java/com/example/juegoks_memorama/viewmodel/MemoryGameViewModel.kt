package com.example.juegoks_memorama.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juegoks_memorama.data.GameRepository
import com.example.juegoks_memorama.model.Card
import com.example.juegoks_memorama.model.GameMode
import com.example.juegoks_memorama.model.GameState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.juegoks_memorama.model.Difficulty
import com.example.juegoks_memorama.model.SaveFormat
import com.example.juegoks_memorama.model.Move
import com.example.juegoks_memorama.model.GameHistoryItem
import com.example.juegoks_memorama.data.SaveFormatSerializer
import com.example.juegoks_memorama.data.SoundPlayer // <-- AÑADIR

data class GameUiState(
    val showSaveDialog: Boolean = false,
    val showHistoryDialog: Boolean = false,
    val historyItems: List<GameHistoryItem> = emptyList(),

    val showPostSaveDialog: Boolean = false,
    val showPostWinSaveDialog: Boolean = false,

    val existingSaveNames: List<String> = emptyList()
)

@HiltViewModel
class MemoryGameViewModel @Inject constructor(
    private val repository: GameRepository,
    private val soundPlayer: SoundPlayer // <-- AÑADIR (Inyectar)
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState = _gameState.asStateFlow()

    private val _gameUiState = MutableStateFlow(GameUiState())
    val gameUiState = _gameUiState.asStateFlow()

    private var firstSelectedCard: Card? = null
    private var secondSelectedCard: Card? = null
    private var canFlip = true
    private var timerJob: Job? = null
    private var currentGameMode: GameMode = GameMode.SINGLE_PLAYER
    private var currentGameDifficulty: Difficulty = Difficulty.MEDIUM

    private var loadedSaveFile: Pair<String, SaveFormat>? = null

    init {
        viewModelScope.launch {
            try {
                val savedGame = repository.savedGameState.firstOrNull()
                if (savedGame != null && !savedGame.gameCompleted) {
                    _gameState.value = savedGame
                    currentGameMode = GameMode.SINGLE_PLAYER
                    currentGameDifficulty = savedGame.difficulty
                    if (savedGame.isTimerRunning) {
                        startTimer()
                    }
                }
            } catch (e: Exception) {
                repository.clearGameState()
            }
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
        if (currentGameDifficulty != difficulty || _gameState.value.cards.isEmpty()) {
            currentGameDifficulty = difficulty
            startNewGame(difficulty = difficulty)
        }
    }

    fun setGameMode(mode: GameMode) {
        if (currentGameMode != mode) {
            currentGameMode = mode
            if (mode == GameMode.SINGLE_PLAYER) {
                startNewGame(difficulty = currentGameDifficulty)
            } else {
                startNewGame(difficulty = Difficulty.MEDIUM)
            }
        } else if (_gameState.value.cards.isEmpty()) {
            startNewGame(difficulty = currentGameDifficulty)
        }
    }

    private fun observeAndSaveGameState() {
        viewModelScope.launch {
            gameState.collect { state ->
                if (currentGameMode == GameMode.SINGLE_PLAYER && !state.gameCompleted && state.moves > 0) {
                    repository.saveGameState(state)
                }
                if(state.gameCompleted){
                    repository.clearGameState()
                }
            }
        }
    }

    fun startNewGame(difficulty: Difficulty = currentGameDifficulty) {
        timerJob?.cancel()
        currentGameDifficulty = difficulty
        loadedSaveFile = null

        val maxPairs = difficulty.pairs
        val cardValues = (1..maxPairs).flatMap { listOf(it, it) }.shuffled()
        val cards = cardValues.mapIndexed { index, value ->
            Card(id = index, value = value)
        }

        _gameState.value = GameState(
            difficulty = difficulty,
            cards = cards,
            score = 0,
            matchStreak = 0,
            moveHistory = emptyList()
        )
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
        if (currentGameMode == GameMode.BLUETOOTH) return

        if (_gameState.value.isTimerRunning) return

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
        if (currentGameMode == GameMode.BLUETOOTH) {
            // Lógica de Bluetooth
        }

        if (!canFlip || card.isFaceUp || card.isMatched) return

        soundPlayer.playFlipSound() // <-- AÑADIR SONIDO DE VOLTEAR
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

        var newScore = currentState.score
        var newMatchStreak = currentState.matchStreak
        var newMoveHistory = currentState.moveHistory

        if (firstCard != null && secondCard != null && firstCard.value == secondCard.value) {
            soundPlayer.playMatchSound() // <-- AÑADIR SONIDO DE ACIERTO

            val firstIndex = updatedCards.indexOfFirst { it.id == firstCard.id }
            val secondIndex = updatedCards.indexOfFirst { it.id == secondCard.id }

            updatedCards[firstIndex] = firstCard.copy(isMatched = true)
            updatedCards[secondIndex] = secondCard.copy(isMatched = true)

            val newMatchedPairs = currentState.matchedPairs + 1
            val gameCompleted = newMatchedPairs == currentState.difficulty.pairs

            newMatchStreak += 1
            val points = 100 * (1 shl (newMatchStreak - 1))
            newScore += points

            newMoveHistory = newMoveHistory + Move(firstCard.id, secondCard.id)

            if (gameCompleted) {
                soundPlayer.playWinSound() // <-- AÑADIR SONIDO DE VICTORIA
                timerJob?.cancel()
                if (currentGameMode == GameMode.BLUETOOTH) {
                    // Lógica para declarar al ganador y terminar la conexión
                }
            }

            _gameState.value = currentState.copy(
                cards = updatedCards,
                matchedPairs = newMatchedPairs,
                score = newScore,
                matchStreak = newMatchStreak,
                moveHistory = newMoveHistory,
                gameCompleted = gameCompleted,
                isTimerRunning = !gameCompleted
            )

            if (currentGameMode == GameMode.BLUETOOTH) {
                // Lógica de "turno extra" o "pasar turno" para Bluetooth
            }
        } else {
            soundPlayer.playNoMatchSound() // <-- AÑADIR SONIDO DE FALLO

            val firstIndex = updatedCards.indexOfFirst { it.id == firstCard?.id }
            val secondIndex = updatedCards.indexOfFirst { it.id == secondCard?.id }

            if (firstIndex != -1) {
                updatedCards[firstIndex] = updatedCards[firstIndex].copy(isFaceUp = false)
            }
            if (secondIndex != -1) {
                updatedCards[secondIndex] = updatedCards[secondIndex].copy(isFaceUp = false)
            }

            newMatchStreak = 0

            _gameState.value = currentState.copy(
                cards = updatedCards,
                matchStreak = newMatchStreak
            )

            if (currentGameMode == GameMode.BLUETOOTH) {
                // Lógica de "pasar turno"
            }
        }

        firstSelectedCard = null
        secondSelectedCard = null
        canFlip = true
    }

    // --- LÓGICA DE GUARDADO/CARGA MANUAL MEJORADA ---

    fun showSaveDialog(show: Boolean) {
        _gameUiState.update { it.copy(showSaveDialog = show) }
    }

    fun showHistoryDialog(show: Boolean) {
        _gameUiState.update { it.copy(showHistoryDialog = show) }
        if (show) {
            viewModelScope.launch {
                val files = repository.getAvailableSaveFiles()
                val currentDifficulty = _gameState.value.difficulty

                val historyList = files.mapNotNull { (filename, format, timestamp) ->
                    val loadedState = repository.loadGameManual(filename, format)

                    if (loadedState != null && loadedState.difficulty == currentDifficulty) {
                        GameHistoryItem(filename, format, loadedState, timestamp)
                    } else {
                        null
                    }
                }

                _gameUiState.update { it.copy(historyItems = historyList) }
            }
        }
    }

    fun onSaveClick() {
        viewModelScope.launch {
            if (loadedSaveFile != null) {
                saveGame(loadedSaveFile!!.first, loadedSaveFile!!.second)
            } else {
                val allNames = repository.getAllSaveFileNames()

                _gameUiState.update {
                    it.copy(
                        existingSaveNames = allNames,
                        showSaveDialog = true
                    )
                }
            }
        }
    }

    suspend fun saveGame(filename: String, format: SaveFormat) {
        val stateToSave = _gameState.value
        repository.saveGameManual(stateToSave, format, filename)

        loadedSaveFile = filename to format

        if (stateToSave.gameCompleted) {
            _gameUiState.update { it.copy(showSaveDialog = false, showPostWinSaveDialog = true) }
        } else {
            _gameUiState.update { it.copy(showSaveDialog = false, showPostSaveDialog = true) }
        }
    }

    fun loadGameFromHistory(filename: String, format: SaveFormat) {
        viewModelScope.launch {
            val loadedState = repository.loadGameManual(filename, format)
            if (loadedState != null) {
                timerJob?.cancel()
                _gameState.value = loadedState
                currentGameDifficulty = loadedState.difficulty

                val filenameWithoutExtension = filename.substringBeforeLast('.')
                loadedSaveFile = filenameWithoutExtension to format

                if (loadedState.isTimerRunning && !loadedState.gameCompleted) {
                    startTimer()
                } else if (!loadedState.isTimerRunning && !loadedState.gameCompleted) {
                    _gameState.update { it.copy(isTimerRunning = false) }
                }
            }
            showHistoryDialog(false)
        }
    }

    fun dismissPostSaveDialog() {
        _gameUiState.update { it.copy(showPostSaveDialog = false) }
    }

    fun dismissPostWinSaveDialog() {
        _gameUiState.update { it.copy(showPostWinSaveDialog = false) }
    }
}