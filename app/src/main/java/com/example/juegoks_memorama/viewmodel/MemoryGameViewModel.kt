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
                }
            } catch (e: Exception) {
                repository.clearGameState()
            }
        }

        viewModelScope.launch {
            bluetoothService.incomingMessages.collect { message ->
                processBluetoothMessage(message)
            }
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
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
                timerJob?.cancel()
                _gameState.update {
                    it.copy(
                        isMultiplayer = true,
                        cards = emptyList(),
                        score = 0,
                        opponentScore = 0,
                        myPairs = 0,
                        opponentPairs = 0
                    )
                }
            }
        }
    }

    fun startNewGame(difficulty: Difficulty = currentGameDifficulty) {
        stopTimer()
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
            isMyTurn = true,
            elapsedTimeInSeconds = 0,
            isTimerRunning = false,
            myPairs = 0,
            opponentPairs = 0
        )
        firstSelectedCard = null
        secondSelectedCard = null
        canFlip = true

        viewModelScope.launch {
            repository.clearGameState()
        }
    }

    fun startMultiplayerGame(isHost: Boolean, difficulty: Difficulty) {
        currentGameMode = GameMode.BLUETOOTH
        currentGameDifficulty = difficulty
        stopTimer()

        val seed = System.currentTimeMillis()

        if (isHost) {
            val maxPairs = difficulty.pairs
            val generatedCardValues = (1..maxPairs).flatMap { listOf(it, it) }.shuffled()

            bluetoothService.sendMessage(BluetoothMessage.StartGame(difficulty, seed, generatedCardValues))
            initializeMultiplayerBoard(difficulty, isHost = true, cardValues = generatedCardValues)
            // En multiplayer, el timer arranca al iniciar la partida para ambos
            startTimer()
        }
    }

    private fun initializeMultiplayerBoard(difficulty: Difficulty, isHost: Boolean, cardValues: List<Int>) {
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
            isMyTurn = isHost,
            myPairs = 0,
            opponentPairs = 0
        )
        firstSelectedCard = null
        secondSelectedCard = null
        canFlip = true
    }

    private fun startTimer() {
        // PERMITIMOS EL TIMER EN BLUETOOTH TAMBIÉN
        if (timerJob?.isActive == true) return

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

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _gameState.update { it.copy(isTimerRunning = false) }
    }

    fun onCardClick(card: Card, fromRemote: Boolean = false) {
        val state = _gameState.value

        if (state.isMultiplayer) {
            if (!fromRemote && !state.isMyTurn) return
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

            if (!state.isMultiplayer || (state.isMultiplayer && state.isMyTurn && !fromRemote)) {
                viewModelScope.launch {
                    delay(1000)
                    checkForMatch()
                }
            }
        }
    }

    private fun checkForMatch() {
        val currentState = _gameState.value
        val updatedCards = currentState.cards.toMutableList()
        val firstCard = firstSelectedCard
        val secondCard = secondSelectedCard

        var newScore = currentState.score
        var isMyTurn = currentState.isMyTurn
        var newMyPairs = currentState.myPairs

        if (firstCard != null && secondCard != null && firstCard.value == secondCard.value) {
            // --- HAY MATCH ---
            soundPlayer.playMatchSound()
            val firstIndex = updatedCards.indexOfFirst { it.id == firstCard.id }
            val secondIndex = updatedCards.indexOfFirst { it.id == secondCard.id }
            updatedCards[firstIndex] = firstCard.copy(isMatched = true)
            updatedCards[secondIndex] = secondCard.copy(isMatched = true)

            val newMatchedPairs = currentState.matchedPairs + 1
            val gameCompleted = newMatchedPairs == currentState.difficulty.pairs

            // CÁLCULO DE PUNTOS: IGUAL PARA SINGLE Y MULTI PLAYER (Base + Streak)
            val newMatchStreak = currentState.matchStreak + 1
            val pointsToAdd = 100 * (1 shl (newMatchStreak - 1))
            newScore += pointsToAdd
            newMyPairs += 1

            if (currentState.isMultiplayer) {
                // Enviamos los puntos calculados para que el otro dispositivo los sume
                bluetoothService.sendMessage(
                    BluetoothMessage.MatchFound(
                        firstCard.id,
                        secondCard.id,
                        scorerIsHost = currentState.isHost,
                        points = pointsToAdd // ENVIAMOS LOS PUNTOS EXACTOS
                    )
                )
            }

            _gameState.value = _gameState.value.copy(
                cards = updatedCards,
                matchedPairs = newMatchedPairs,
                score = newScore,
                matchStreak = newMatchStreak, // Actualizamos racha
                gameCompleted = gameCompleted,
                isMyTurn = isMyTurn,
                myPairs = newMyPairs // Actualizamos mis pares
            )

            if (gameCompleted) {
                soundPlayer.playWinSound()
                stopTimer()
                viewModelScope.launch { repository.clearGameState() }
            }

        } else {
            // --- NO HAY MATCH ---
            soundPlayer.playNoMatchSound()
            val firstIndex = updatedCards.indexOfFirst { it.id == firstCard?.id }
            val secondIndex = updatedCards.indexOfFirst { it.id == secondCard?.id }
            if (firstIndex != -1) updatedCards[firstIndex] = updatedCards[firstIndex].copy(isFaceUp = false)
            if (secondIndex != -1) updatedCards[secondIndex] = updatedCards[secondIndex].copy(isFaceUp = false)

            if (currentState.isMultiplayer) {
                isMyTurn = false
                val nextIsHost = !currentState.isHost
                bluetoothService.sendMessage(BluetoothMessage.TurnChange(nextTurnIsHost = nextIsHost))
            }

            _gameState.value = currentState.copy(
                cards = updatedCards,
                matchStreak = 0, // Reinicia racha
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
                currentGameMode = GameMode.BLUETOOTH
                currentGameDifficulty = msg.difficulty
                initializeMultiplayerBoard(msg.difficulty, isHost = false, cardValues = msg.cardValues)
                startTimer() // Cliente inicia timer al recibir StartGame
            }
            is BluetoothMessage.FlipCard -> {
                val card = _gameState.value.cards.find { it.id == msg.cardId }
                if (card != null) onCardClick(card, fromRemote = true)
            }
            is BluetoothMessage.MatchFound -> {
                val currentCards = _gameState.value.cards.toMutableList()
                val c1Index = currentCards.indexOfFirst { it.id == msg.card1Id }
                val c2Index = currentCards.indexOfFirst { it.id == msg.card2Id }

                if(c1Index != -1) currentCards[c1Index] = currentCards[c1Index].copy(isMatched = true, isFaceUp = true)
                if(c2Index != -1) currentCards[c2Index] = currentCards[c2Index].copy(isMatched = true, isFaceUp = true)

                val matchedCount = currentCards.count { it.isMatched } / 2
                val isGameCompleted = matchedCount == _gameState.value.difficulty.pairs

                if (isGameCompleted) {
                    soundPlayer.playWinSound()
                    stopTimer()
                } else {
                    soundPlayer.playMatchSound()
                }

                _gameState.update {
                    it.copy(
                        cards = currentCards,
                        opponentScore = it.opponentScore + msg.points, // Sumamos los puntos que calculó el rival
                        opponentPairs = it.opponentPairs + 1, // Sumamos par al rival
                        matchedPairs = matchedCount,
                        gameCompleted = isGameCompleted
                    )
                }
                firstSelectedCard = null
                secondSelectedCard = null
                canFlip = true
            }
            is BluetoothMessage.TurnChange -> {
                soundPlayer.playNoMatchSound()
                val currentCards = _gameState.value.cards.toMutableList()
                val openCards = currentCards.filter { it.isFaceUp && !it.isMatched }
                openCards.forEach { card ->
                    val idx = currentCards.indexOfFirst { it.id == card.id }
                    if (idx != -1) currentCards[idx] = currentCards[idx].copy(isFaceUp = false)
                }

                val amIHost = _gameState.value.isHost
                val isNowMyTurn = (msg.nextTurnIsHost && amIHost) || (!msg.nextTurnIsHost && !amIHost)

                _gameState.update {
                    it.copy(
                        cards = currentCards,
                        isMyTurn = isNowMyTurn
                    )
                }
                firstSelectedCard = null
                secondSelectedCard = null
                canFlip = true
            }
            is BluetoothMessage.RestartGame -> {}
        }
    }

    fun onExitGame() {
        stopTimer()
        viewModelScope.launch { repository.clearGameState() }
    }

    fun showSaveDialog(show: Boolean) {
        if (currentGameMode != GameMode.SINGLE_PLAYER) return
        viewModelScope.launch {
            val allNames = repository.getAllSaveFileNames()
            _gameUiState.update { it.copy(showSaveDialog = show, existingSaveNames = allNames) }
        }
    }

    fun showHistoryDialog(show: Boolean) {
        if (currentGameMode != GameMode.SINGLE_PLAYER) return
        _gameUiState.update { it.copy(showHistoryDialog = show) }
        if (show) {
            viewModelScope.launch {
                val files = repository.getAvailableSaveFiles()
                val historyList = files.mapNotNull { (filename, format, timestamp) ->
                    val loadedState = repository.loadGameManual(filename, format)
                    if (loadedState != null) {
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
                _gameUiState.update { it.copy(existingSaveNames = allNames, showSaveDialog = true) }
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
                stopTimer()
                _gameState.value = loadedState
                currentGameDifficulty = loadedState.difficulty
                currentGameMode = GameMode.SINGLE_PLAYER
                val filenameWithoutExtension = filename.substringBeforeLast('.')
                loadedSaveFile = filenameWithoutExtension to format
                _gameState.update { it.copy(isTimerRunning = false) }
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