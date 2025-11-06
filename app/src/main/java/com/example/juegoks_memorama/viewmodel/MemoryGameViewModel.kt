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
import com.example.juegoks_memorama.model.Difficulty // Importar
import com.example.juegoks_memorama.model.SaveFormat // Importar
import com.example.juegoks_memorama.model.Move
import com.example.juegoks_memorama.model.GameHistoryItem
import com.example.juegoks_memorama.data.SaveFormatSerializer // Importar

data class GameUiState(
    val showSaveDialog: Boolean = false,
    val showHistoryDialog: Boolean = false, // CAMBIO: showLoadDialog -> showHistoryDialog
    val historyItems: List<GameHistoryItem> = emptyList(), // CAMBIO: saveFiles -> historyItems

    // --- NUEVOS ESTADOS PARA DIÁLOGOS POST-GUARDADO ---
    val showPostSaveDialog: Boolean = false, // Diálogo después de guardar (en curso)
    val showPostWinSaveDialog: Boolean = false, // Diálogo después de guardar (partida ganada)

    // --- NUEVO: Para validación de nombres de archivo ---
    val existingSaveNames: List<String> = emptyList()
)

@HiltViewModel
class MemoryGameViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    private val _gameState = MutableStateFlow(GameState())
    val gameState = _gameState.asStateFlow()

    private val _gameUiState = MutableStateFlow(GameUiState()) // Nuevo StateFlow para la UI
    val gameUiState = _gameUiState.asStateFlow()

    private var firstSelectedCard: Card? = null
    private var secondSelectedCard: Card? = null
    private var canFlip = true
    private var timerJob: Job? = null
    private var currentGameMode: GameMode = GameMode.SINGLE_PLAYER
    private var currentGameDifficulty: Difficulty = Difficulty.MEDIUM // Nuevo campo para la dificultad

    // --- NUEVO: Rastrear el archivo cargado para sobrescribir ---
    private var loadedSaveFile: Pair<String, SaveFormat>? = null

    init {
        viewModelScope.launch {
            try {
                // CORRECCIÓN DE CRASH: Intentar cargar la partida guardada
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
                // Si el JSON es incompatible (modelo de datos antiguo), limpiamos el guardado
                // para evitar futuros crasheos y forzar un estado limpio.
                repository.clearGameState()
            }
        }
    }

    // --- NUEVO: Establecer Dificultad ---
    fun setDifficulty(difficulty: Difficulty) {
        // Corrección: Asegura que el juego se inicie si la dificultad es diferente o si es la primera carga.
        if (currentGameDifficulty != difficulty || _gameState.value.cards.isEmpty()) {
            currentGameDifficulty = difficulty
            startNewGame(difficulty = difficulty)
        }
    }
    // ------------------------------------

    fun setGameMode(mode: GameMode) {
        if (currentGameMode != mode) {
            currentGameMode = mode
            if (mode == GameMode.SINGLE_PLAYER) {
                startNewGame(difficulty = currentGameDifficulty)
            } else {
                // Para Bluetooth, se usa la dificultad media por defecto
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

    // --- MODIFICADO: Aceptar Dificultad como parámetro ---
    fun startNewGame(difficulty: Difficulty = currentGameDifficulty) {
        timerJob?.cancel()
        currentGameDifficulty = difficulty
        loadedSaveFile = null // <-- NUEVO: Reiniciar al empezar nuevo juego

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
            val firstIndex = updatedCards.indexOfFirst { it.id == firstCard.id }
            val secondIndex = updatedCards.indexOfFirst { it.id == secondCard.id }

            updatedCards[firstIndex] = firstCard.copy(isMatched = true)
            updatedCards[secondIndex] = secondCard.copy(isMatched = true)

            val newMatchedPairs = currentState.matchedPairs + 1
            val gameCompleted = newMatchedPairs == currentState.difficulty.pairs

            // --- LÓGICA DE PUNTUACIÓN Y RACHA ---
            newMatchStreak += 1
            // Puntuación: 100 * 2^(racha - 1)
            val points = 100 * (1 shl (newMatchStreak - 1))
            newScore += points
            // ------------------------------------

            // --- ACTUALIZAR HISTORIAL DE MOVIMIENTOS ---
            newMoveHistory = newMoveHistory + Move(firstCard.id, secondCard.id) // CORREGIDO: Usar Move
            // --------------------------------------------

            if (gameCompleted) {
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
            val firstIndex = updatedCards.indexOfFirst { it.id == firstCard?.id }
            val secondIndex = updatedCards.indexOfFirst { it.id == secondCard?.id }

            if (firstIndex != -1) {
                updatedCards[firstIndex] = updatedCards[firstIndex].copy(isFaceUp = false)
            }
            if (secondIndex != -1) {
                updatedCards[secondIndex] = updatedCards[secondIndex].copy(isFaceUp = false)
            }

            // --- LÓGICA DE RACHA: Reiniciar si no hay match ---
            newMatchStreak = 0
            // --------------------------------------------------

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

    // CAMBIO: Renombrado y lógica nueva
    fun showHistoryDialog(show: Boolean) {
        _gameUiState.update { it.copy(showHistoryDialog = show) }
        if (show) {
            viewModelScope.launch {
                // Obtenemos los nombres de archivo, formato y timestamp
                val files = repository.getAvailableSaveFiles()
                val currentDifficulty = _gameState.value.difficulty // Dificultad actual

                // Cargamos el estado de CADA archivo para clasificarlo
                val historyList = files.mapNotNull { (filename, format, timestamp) ->
                    val loadedState = repository.loadGameManual(filename, format)

                    // --- REQ: FILTRAR POR DIFICULTAD ---
                    if (loadedState != null && loadedState.difficulty == currentDifficulty) {
                        // Usamos el 'filename' con extensión
                        GameHistoryItem(filename, format, loadedState, timestamp)
                    } else {
                        null
                    }
                }

                _gameUiState.update { it.copy(historyItems = historyList) }
            }
        }
    }

    // --- NUEVO: Lógica para decidir si sobrescribir o "Guardar como" ---
    fun onSaveClick() {
        viewModelScope.launch {
            if (loadedSaveFile != null) {
                // Sobrescribir: Guardar directamente con el nombre y formato ya conocidos
                // El nombre en loadedSaveFile NO tiene extensión
                saveGame(loadedSaveFile!!.first, loadedSaveFile!!.second)
            } else {
                // Guardar como:
                // 1. Obtener todos los nombres existentes (sin extensión)
                val allNames = repository.getAllSaveFileNames()

                // 2. Actualizar el state con los nombres y mostrar el diálogo
                _gameUiState.update {
                    it.copy(
                        existingSaveNames = allNames,
                        showSaveDialog = true
                    )
                }
            }
        }
    }

    // CAMBIO: Convertido a suspend fun y muestra el diálogo post-guardado
    suspend fun saveGame(filename: String, format: SaveFormat) {
        val stateToSave = _gameState.value
        // Guardamos usando el nombre de archivo SIN extensión
        repository.saveGameManual(stateToSave, format, filename)

        // Guardar el archivo que acabamos de usar (sin extensión)
        loadedSaveFile = filename to format

        // Mostrar el diálogo post-guardado correcto
        if (stateToSave.gameCompleted) {
            // REQ: Mostrar diálogo de "Jugar de nuevo" o "Salir"
            _gameUiState.update { it.copy(showSaveDialog = false, showPostWinSaveDialog = true) }
        } else {
            // REQ: Mostrar diálogo de "Continuar", "Nuevo" o "Salir"
            _gameUiState.update { it.copy(showSaveDialog = false, showPostSaveDialog = true) }
        }
    }

    // CAMBIO: Renombrado y guarda el archivo cargado
    fun loadGameFromHistory(filename: String, format: SaveFormat) {
        viewModelScope.launch {
            // 'filename' aquí SÍ tiene extensión (ej: "partida1.json")
            val loadedState = repository.loadGameManual(filename, format)
            if (loadedState != null) {
                timerJob?.cancel()
                _gameState.value = loadedState
                currentGameDifficulty = loadedState.difficulty // Cargar la dificultad del guardado

                // --- NUEVO: Guardar referencia al archivo cargado (SIN extensión) ---
                val filenameWithoutExtension = filename.substringBeforeLast('.')
                loadedSaveFile = filenameWithoutExtension to format

                // Si se carga un juego incompleto, se debe iniciar el timer si estaba activo
                if (loadedState.isTimerRunning && !loadedState.gameCompleted) {
                    startTimer()
                } else if (!loadedState.isTimerRunning && !loadedState.gameCompleted) {
                    // Asegurar que el juego pueda reanudarse
                    _gameState.update { it.copy(isTimerRunning = false) }
                }
            }
            showHistoryDialog(false) // Cierra el diálogo de historial
        }
    }

    // --- NUEVO: Funciones para cerrar los diálogos post-guardado ---
    fun dismissPostSaveDialog() {
        _gameUiState.update { it.copy(showPostSaveDialog = false) }
    }

    fun dismissPostWinSaveDialog() {
        _gameUiState.update { it.copy(showPostWinSaveDialog = false) }
    }
}