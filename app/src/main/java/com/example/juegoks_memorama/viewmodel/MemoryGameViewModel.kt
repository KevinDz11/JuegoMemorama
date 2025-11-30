package com.example.juegoks_memorama.viewmodel

import android.util.Log
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
import com.example.juegoks_memorama.data.SoundPlayer
import com.example.juegoks_memorama.model.AppThemeOption
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import com.example.juegoks_memorama.data.BluetoothService
import com.example.juegoks_memorama.model.BluetoothMessage

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
    private val soundPlayer: SoundPlayer,
    val bluetoothService: BluetoothService
) : ViewModel() {

    private val TAG = "MemoryGameViewModel"

    val currentTheme = repository.savedTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppThemeOption.IPN
        )

    fun setTheme(theme: AppThemeOption) {
        viewModelScope.launch {
            repository.saveTheme(theme)
        }
    }

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

        viewModelScope.launch {
            bluetoothService.incomingMessages.collect { message ->
                Log.d(TAG, "Mensaje recibido: $message")
                processBluetoothMessage(message)
            }
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
        // CORRECCIÓN: Asegurar que NO se reinicie el juego si estamos en Bluetooth
        if (currentGameMode == GameMode.SINGLE_PLAYER) {
            if (currentGameDifficulty != difficulty || _gameState.value.cards.isEmpty()) {
                currentGameDifficulty = difficulty
                startNewGame(difficulty = difficulty)
            }
        }
    }

    fun setGameMode(mode: GameMode) {
        if (currentGameMode != mode) {
            currentGameMode = mode
            if (mode == GameMode.SINGLE_PLAYER) {
                startNewGame(difficulty = currentGameDifficulty)
            } else {
                // CORRECCIÓN: Si cambiamos a Bluetooth, limpiamos el tablero inmediatamente
                // para evitar ver cartas aleatorias mientras esperamos la conexión/datos.
                _gameState.update {
                    it.copy(
                        isMultiplayer = true,
                        cards = emptyList(), // Tablero vacío hasta recibir StartGame
                        score = 0,
                        opponentScore = 0
                    )
                }
            }
        }
    }

    // --- LÓGICA UN JUGADOR ---
    fun startNewGame(difficulty: Difficulty = currentGameDifficulty) {
        timerJob?.cancel()
        currentGameDifficulty = difficulty
        loadedSaveFile = null
        currentGameMode = GameMode.SINGLE_PLAYER

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
            moveHistory = emptyList(),
            isMultiplayer = false,
            isMyTurn = true
        )
        firstSelectedCard = null
        secondSelectedCard = null
        canFlip = true

        viewModelScope.launch {
            repository.clearGameState()
        }
    }

    // --- LÓGICA MULTIJUGADOR ---
    fun startMultiplayerGame(isHost: Boolean, difficulty: Difficulty) {
        currentGameMode = GameMode.BLUETOOTH
        currentGameDifficulty = difficulty
        timerJob?.cancel()

        val seed = System.currentTimeMillis()

        if (isHost) {
            Log.d(TAG, "Iniciando juego como HOST.")

            // 1. EL HOST GENERA LA LISTA ALEATORIA
            val maxPairs = difficulty.pairs
            val generatedCardValues = (1..maxPairs).flatMap { listOf(it, it) }.shuffled()

            // 2. EL HOST ENVÍA LA LISTA EXACTA AL CLIENTE
            bluetoothService.sendMessage(BluetoothMessage.StartGame(difficulty, seed, generatedCardValues))

            // 3. EL HOST INICIALIZA SU TABLERO CON ESA LISTA
            initializeMultiplayerBoard(difficulty, isHost = true, cardValues = generatedCardValues)
        } else {
            // El cliente no hace nada aquí, espera a recibir el mensaje StartGame
            // para llamar a initializeMultiplayerBoard con la lista correcta.
        }
    }

    // Usamos la lista de valores recibida (cardValues), NO Random()
    private fun initializeMultiplayerBoard(difficulty: Difficulty, isHost: Boolean, cardValues: List<Int>) {
        Log.d(TAG, "Inicializando tablero multijugador. Items: ${cardValues.size}")

        val cards = cardValues.mapIndexed { index, value ->
            Card(id = index, value = value)
        }

        _gameState.value = GameState(
            difficulty = difficulty,
            cards = cards,
            score = 0,
            opponentScore = 0,
            matchStreak = 0,
            moveHistory = emptyList(),
            isMultiplayer = true,
            isHost = isHost,
            isMyTurn = isHost // El Host siempre empieza
        )
        firstSelectedCard = null
        secondSelectedCard = null
        canFlip = true
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

    fun onCardClick(card: Card, fromRemote: Boolean = false) {
        val state = _gameState.value

        if (state.isMultiplayer) {
            if (!fromRemote && !state.isMyTurn) return
            if (fromRemote && state.isMyTurn) return
        }

        if (!canFlip || card.isFaceUp || card.isMatched) return

        soundPlayer.playFlipSound()

        if (state.isMultiplayer && !fromRemote) {
            bluetoothService.sendMessage(BluetoothMessage.FlipCard(card.id))
        }

        if (!state.isMultiplayer) startTimer()

        val updatedCards = state.cards.toMutableList()
        val cardIndex = updatedCards.indexOfFirst { it.id == card.id }

        if (cardIndex == -1) return

        updatedCards[cardIndex] = card.copy(isFaceUp = true)

        if (firstSelectedCard == null) {
            firstSelectedCard = updatedCards[cardIndex]
            _gameState.value = state.copy(cards = updatedCards)
        } else {
            secondSelectedCard = updatedCards[cardIndex]
            canFlip = false

            _gameState.value = state.copy(
                cards = updatedCards,
                moves = state.moves + 1
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
        var newOpponentScore = currentState.opponentScore
        var newMatchStreak = currentState.matchStreak
        var newMoveHistory = currentState.moveHistory
        var isMyTurn = currentState.isMyTurn

        if (firstCard != null && secondCard != null && firstCard.value == secondCard.value) {
            soundPlayer.playMatchSound()
            val firstIndex = updatedCards.indexOfFirst { it.id == firstCard.id }
            val secondIndex = updatedCards.indexOfFirst { it.id == secondCard.id }
            updatedCards[firstIndex] = firstCard.copy(isMatched = true)
            updatedCards[secondIndex] = secondCard.copy(isMatched = true)

            val newMatchedPairs = currentState.matchedPairs + 1
            val gameCompleted = newMatchedPairs == currentState.difficulty.pairs

            if (currentState.isMultiplayer) {
                val points = 100
                if (isMyTurn) {
                    newScore += points
                    bluetoothService.sendMessage(BluetoothMessage.MatchFound(firstCard.id, secondCard.id, scorerIsHost = currentState.isHost))
                } else {
                    newOpponentScore += points
                }
            } else {
                newMatchStreak += 1
                val points = 100 * (1 shl (newMatchStreak - 1))
                newScore += points
            }

            newMoveHistory = newMoveHistory + Move(firstCard.id, secondCard.id)
            if (gameCompleted) {
                soundPlayer.playWinSound()
                timerJob?.cancel()
            }

            _gameState.value = currentState.copy(
                cards = updatedCards,
                matchedPairs = newMatchedPairs,
                score = newScore,
                opponentScore = newOpponentScore,
                matchStreak = newMatchStreak,
                moveHistory = newMoveHistory,
                gameCompleted = gameCompleted,
                isTimerRunning = !gameCompleted,
                isMyTurn = isMyTurn
            )

        } else {
            soundPlayer.playNoMatchSound()
            val firstIndex = updatedCards.indexOfFirst { it.id == firstCard?.id }
            val secondIndex = updatedCards.indexOfFirst { it.id == secondCard?.id }
            if (firstIndex != -1) updatedCards[firstIndex] = updatedCards[firstIndex].copy(isFaceUp = false)
            if (secondIndex != -1) updatedCards[secondIndex] = updatedCards[secondIndex].copy(isFaceUp = false)

            if (currentState.isMultiplayer) {
                if (isMyTurn) {
                    isMyTurn = false
                    val nextIsHost = !currentState.isHost
                    bluetoothService.sendMessage(BluetoothMessage.TurnChange(nextTurnIsHost = nextIsHost))
                } else {
                    isMyTurn = true
                }
            } else {
                newMatchStreak = 0
            }

            _gameState.value = currentState.copy(
                cards = updatedCards,
                matchStreak = newMatchStreak,
                isMyTurn = isMyTurn
            )
        }
        firstSelectedCard = null
        secondSelectedCard = null
        canFlip = true
    }

    private fun processBluetoothMessage(msg: BluetoothMessage) {
        when (msg) {
            is BluetoothMessage.StartGame -> {
                Log.d(TAG, "Recibido StartGame. Configurando tablero Cliente.")
                currentGameMode = GameMode.BLUETOOTH
                currentGameDifficulty = msg.difficulty
                // EL CLIENTE USA LA LISTA DEL HOST
                initializeMultiplayerBoard(msg.difficulty, isHost = false, cardValues = msg.cardValues)
            }
            is BluetoothMessage.FlipCard -> {
                val card = _gameState.value.cards.find { it.id == msg.cardId }
                if (card != null) onCardClick(card, fromRemote = true)
            }
            is BluetoothMessage.MatchFound -> { /* Sincronizado en checkForMatch */ }
            is BluetoothMessage.TurnChange -> {
                val amIHost = _gameState.value.isHost
                val isNowMyTurn = (msg.nextTurnIsHost && amIHost) || (!msg.nextTurnIsHost && !amIHost)
                if (_gameState.value.isMyTurn != isNowMyTurn) {
                    _gameState.update { it.copy(isMyTurn = isNowMyTurn) }
                }
            }
            is BluetoothMessage.RestartGame -> {}
        }
    }

    // ... (El resto de funciones showSaveDialog, etc. se mantienen igual) ...
    // COPIA AQUÍ EL RESTO DEL CÓDIGO (SaveDialogs, etc.) DEL ARCHIVO ANTERIOR
    // SON LAS MISMAS FUNCIONES DE GUARDADO MANUAL QUE YA TIENES.

    fun showSaveDialog(show: Boolean) {
        if (currentGameMode != GameMode.SINGLE_PLAYER) return
        _gameUiState.update { it.copy(showSaveDialog = show) }
    }

    fun showHistoryDialog(show: Boolean) {
        if (currentGameMode != GameMode.SINGLE_PLAYER) return
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
        if (currentGameMode != GameMode.SINGLE_PLAYER) return
        viewModelScope.launch {
            if (loadedSaveFile != null) {
                saveGame(loadedSaveFile!!.first, loadedSaveFile!!.second)
            } else {
                val allNames = repository.getAllSaveFileNames()
                _gameUiState.update {
                    it.copy(existingSaveNames = allNames, showSaveDialog = true)
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
                currentGameMode = GameMode.SINGLE_PLAYER
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