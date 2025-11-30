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
// --- IMPORTS NUEVOS PARA BLUETOOTH ---
import com.example.juegoks_memorama.data.BluetoothService
import com.example.juegoks_memorama.model.BluetoothMessage
import java.util.Random

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
    val bluetoothService: BluetoothService // <--- NUEVO: Inyectamos el servicio
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

        // --- NUEVO: Escuchar mensajes Bluetooth entrantes ---
        viewModelScope.launch {
            bluetoothService.incomingMessages.collect { message ->
                Log.d(TAG, "Mensaje recibido: $message")
                processBluetoothMessage(message)
            }
        }
    }

    fun setDifficulty(difficulty: Difficulty) {
        // Solo aplica cambios si estamos en modo Un Jugador o si el juego está vacío
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
                // Para Bluetooth, no iniciamos el tablero inmediatamente, esperamos conexión
                _gameState.update { it.copy(isMultiplayer = true) }
            }
        }
    }

    private fun observeAndSaveGameState() {
        viewModelScope.launch {
            gameState.collect { state ->
                // Solo guardamos automáticamente en modo Un Jugador
                if (currentGameMode == GameMode.SINGLE_PLAYER && !state.gameCompleted && state.moves > 0) {
                    repository.saveGameState(state)
                }
                if(state.gameCompleted && currentGameMode == GameMode.SINGLE_PLAYER){
                    repository.clearGameState()
                }
            }
        }
    }

    // --- LÓGICA ORIGINAL UN JUGADOR ---
    fun startNewGame(difficulty: Difficulty = currentGameDifficulty) {
        timerJob?.cancel()
        currentGameDifficulty = difficulty
        loadedSaveFile = null
        currentGameMode = GameMode.SINGLE_PLAYER // Aseguramos modo local

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
            isMyTurn = true // En un jugador siempre es mi turno
        )
        firstSelectedCard = null
        secondSelectedCard = null
        canFlip = true

        viewModelScope.launch {
            repository.clearGameState()
        }
    }

    // --- NUEVA LÓGICA MULTIJUGADOR ---
    fun startMultiplayerGame(isHost: Boolean, difficulty: Difficulty) {
        currentGameMode = GameMode.BLUETOOTH
        currentGameDifficulty = difficulty
        timerJob?.cancel()

        val seed = System.currentTimeMillis()

        if (isHost) {
            Log.d(TAG, "Iniciando juego como HOST. Semilla: $seed")
            // Generamos configuración y enviamos al cliente
            bluetoothService.sendMessage(BluetoothMessage.StartGame(difficulty, seed))
            initializeMultiplayerBoard(difficulty, seed, isHost = true)
        } else {
            Log.d(TAG, "Esperando configuración del HOST...")
            // Si no es Host, espera el mensaje StartGame.
            // Mientras tanto, podemos mostrar un estado de carga o vacío.
            _gameState.value = GameState(
                isMultiplayer = true,
                isHost = false,
                isMyTurn = false,
                cards = emptyList() // Tablero vacío hasta recibir datos
            )
        }
    }

    private fun initializeMultiplayerBoard(difficulty: Difficulty, seed: Long, isHost: Boolean) {
        Log.d(TAG, "Inicializando tablero multijugador. Host: $isHost, Semilla: $seed")
        val random = Random(seed)
        val maxPairs = difficulty.pairs
        // Usamos la semilla para que el shuffle sea idéntico en ambos dispositivos
        val cardValues = (1..maxPairs).flatMap { listOf(it, it) }.shuffled(random)
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
        if (currentGameMode == GameMode.BLUETOOTH) return // Sin timer global en BT por ahora

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

    // --- MODIFICADO PARA SOPORTAR CLICK REMOTO ---
    fun onCardClick(card: Card, fromRemote: Boolean = false) {
        val state = _gameState.value

        // VALIDACIÓN DE TURNO Y BLOQUEO MULTIJUGADOR
        if (state.isMultiplayer) {
            // Si es mi pantalla y NO es mi turno, ignorar click
            if (!fromRemote && !state.isMyTurn) {
                Log.d(TAG, "Click ignorado: No es mi turno")
                return
            }
            // Si viene de remoto pero ES mi turno (error de sincronía), ignorar
            if (fromRemote && state.isMyTurn) {
                Log.d(TAG, "Evento remoto ignorado: Es mi turno")
                return
            }
        }

        if (!canFlip || card.isFaceUp || card.isMatched) return

        soundPlayer.playFlipSound()

        // Si es un click local en multiplayer, avisar al otro
        if (state.isMultiplayer && !fromRemote) {
            Log.d(TAG, "Enviando FlipCard: ${card.id}")
            bluetoothService.sendMessage(BluetoothMessage.FlipCard(card.id))
        }

        // Lógica común de Timer (solo single player por ahora)
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

        // Variables auxiliares
        var newScore = currentState.score
        var newOpponentScore = currentState.opponentScore
        var newMatchStreak = currentState.matchStreak
        var newMoveHistory = currentState.moveHistory
        var isMyTurn = currentState.isMyTurn

        if (firstCard != null && secondCard != null && firstCard.value == secondCard.value) {
            // --- HAY MATCH ---
            soundPlayer.playMatchSound()

            val firstIndex = updatedCards.indexOfFirst { it.id == firstCard.id }
            val secondIndex = updatedCards.indexOfFirst { it.id == secondCard.id }

            updatedCards[firstIndex] = firstCard.copy(isMatched = true)
            updatedCards[secondIndex] = secondCard.copy(isMatched = true)

            val newMatchedPairs = currentState.matchedPairs + 1
            val gameCompleted = newMatchedPairs == currentState.difficulty.pairs

            // Cálculo de Puntuación
            if (currentState.isMultiplayer) {
                // En BT, sumamos puntos a quien tenga el turno ACTUALMENTE
                val points = 100 // Puntuación plana para simplificar BT
                if (isMyTurn) {
                    newScore += points
                    // Solo el que tiene el turno (y por ende ejecutó la lógica localmente primero) envía el MatchFound
                    // PERO espera, ambos ejecutan checkForMatch independientemente.
                    // Para evitar duplicados, confiamos en que FlipCard sincronizó el estado visual.
                    // Podemos enviar MatchFound como confirmación o simplemente confiar en la lógica determinista.
                    // Enviemos MatchFound para estar seguros y sincronizar scores explícitamente si queremos.
                    Log.d(TAG, "Match encontrado (local). Enviando MatchFound.")
                    bluetoothService.sendMessage(BluetoothMessage.MatchFound(firstCard.id, secondCard.id))
                } else {
                    newOpponentScore += points
                    Log.d(TAG, "Match encontrado (remoto).")
                }
                // En BT, si aciertas, conservas el turno. No cambia isMyTurn.
            } else {
                // Lógica Single Player original
                newMatchStreak += 1
                val points = 100 * (1 shl (newMatchStreak - 1))
                newScore += points
            }

            newMoveHistory = newMoveHistory + Move(firstCard.id, secondCard.id)

            if (gameCompleted) {
                soundPlayer.playWinSound()
                timerJob?.cancel()
                // Aquí podrías agregar lógica de quién ganó en BT
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
                isMyTurn = isMyTurn // Se mantiene
            )

        } else {
            // --- NO HAY MATCH ---
            soundPlayer.playNoMatchSound()

            val firstIndex = updatedCards.indexOfFirst { it.id == firstCard?.id }
            val secondIndex = updatedCards.indexOfFirst { it.id == secondCard?.id }

            if (firstIndex != -1) updatedCards[firstIndex] = updatedCards[firstIndex].copy(isFaceUp = false)
            if (secondIndex != -1) updatedCards[secondIndex] = updatedCards[secondIndex].copy(isFaceUp = false)

            if (currentState.isMultiplayer) {
                if (isMyTurn) {
                    // Si era mi turno y fallé, pierdo el turno y aviso
                    isMyTurn = false
                    val nextIsHost = !currentState.isHost
                    Log.d(TAG, "No hubo match (local). Cambio de turno. Próximo Host: $nextIsHost")
                    bluetoothService.sendMessage(BluetoothMessage.TurnChange(nextTurnIsHost = nextIsHost))
                } else {
                    // Si era turno del otro y falló, ahora es mío
                    isMyTurn = true
                    Log.d(TAG, "No hubo match (remoto). Ahora es mi turno.")
                }
            } else {
                newMatchStreak = 0 // Single player resetea racha
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

    // --- PROCESAMIENTO DE MENSAJES BLUETOOTH ---
    private fun processBluetoothMessage(msg: BluetoothMessage) {
        when (msg) {
            is BluetoothMessage.StartGame -> {
                Log.d(TAG, "Recibido StartGame. Semilla: ${msg.seed}")
                // Cliente recibe configuración
                // IMPORTANTE: Aseguramos que estamos en modo BT
                currentGameMode = GameMode.BLUETOOTH
                currentGameDifficulty = msg.difficulty
                initializeMultiplayerBoard(msg.difficulty, msg.seed, isHost = false)
            }
            is BluetoothMessage.FlipCard -> {
                Log.d(TAG, "Recibido FlipCard: ${msg.cardId}")
                // Simular click del oponente
                val card = _gameState.value.cards.find { it.id == msg.cardId }
                if (card != null) {
                    onCardClick(card, fromRemote = true)
                }
            }
            is BluetoothMessage.MatchFound -> {
                Log.d(TAG, "Recibido MatchFound.")
                // El oponente confirma un match.
                // Como ya ejecutamos FlipCard x2 remotamente, checkForMatch ya debió correr o va a correr.
                // Podemos usar esto para forzar sincronización de score si hubo desvío.
            }
            is BluetoothMessage.TurnChange -> {
                Log.d(TAG, "Recibido TurnChange.")
                // El oponente falló, verificar si me toca
                val amIHost = _gameState.value.isHost
                val isNowMyTurn = (msg.nextTurnIsHost && amIHost) || (!msg.nextTurnIsHost && !amIHost)

                // Forzar actualización si hubo desincronización
                if (_gameState.value.isMyTurn != isNowMyTurn) {
                    _gameState.update { it.copy(isMyTurn = isNowMyTurn) }
                }
            }
        }
    }

    // --- LÓGICA DE GUARDADO/CARGA MANUAL (Sin cambios, solo funciona en Single Player) ---

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
                currentGameMode = GameMode.SINGLE_PLAYER // Forzar single player al cargar

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