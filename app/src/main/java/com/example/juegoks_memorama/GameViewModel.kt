package com.example.juegoks_memorama // Tu nombre de paquete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.serialization.encodeToString // Asegúrate de que este import exista
import kotlinx.serialization.json.Json

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val prefs = application.getSharedPreferences("memorama_prefs", Application.MODE_PRIVATE)
    private val SAVED_GAME_KEY = "saved_game_state"


    init {
        // Intenta cargar el juego al iniciar
        if (!loadGame()) {
            // Si no hay juego guardado, inicia uno nuevo
            startNewGame()
        }
    }

    private fun startNewGame() {
        val gameCards = (CardValues.icons + CardValues.icons) // Duplica la lista de iconos
            .shuffled() // Mezcla las cartas
            .mapIndexed { index, icon -> // Crea los objetos MemoryCard
                MemoryCard(
                    id = index,
                    imageVector = icon
                )
            }
        _uiState.value = GameUiState(cards = gameCards)
        // Borra la partida guardada al crear una nueva
        prefs.edit().remove(SAVED_GAME_KEY).apply()
    }

    fun resetGame() {
        startNewGame()
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
            saveGame()
        }
    }
    private fun saveGame() {
        // 1. Convierte el estado actual a un estado "guardable"
        val currentState = _uiState.value
        val savableCards = currentState.cards.map { card ->
            SavedCardState(
                // Guarda el índice del ícono, no el objeto ImageVector
                iconIndex = CardValues.icons.indexOf(card.imageVector),
                isFaceUp = false, // Siempre guarda boca abajo
                isMatched = card.isMatched,
                id = card.id
            )
        }
        val savedState = SavedGameState(cards = savableCards, score = currentState.score)

        // 2. Convierte el estado a un String JSON
        val jsonString = Json.encodeToString(savedState)

        // 3. Guarda el string en SharedPreferences
        prefs.edit().putString(SAVED_GAME_KEY, jsonString).apply()
    }
    private fun loadGame(): Boolean {
        // 1. Lee el string JSON de SharedPreferences
        val jsonString = prefs.getString(SAVED_GAME_KEY, null) ?: return false

        try {
            // 2. Convierte el JSON de vuelta a nuestro objeto SavedGameState
            val savedState = Json.decodeFromString<SavedGameState>(jsonString)

            // 3. Reconstruye el estado del juego (GameUiState) desde el estado guardado
            val loadedCards = savedState.cards.map { savedCard ->
                MemoryCard(
                    id = savedCard.id,
                    // Recupera el ImageVector usando el índice guardado
                    imageVector = CardValues.icons[savedCard.iconIndex],
                    isFaceUp = savedCard.isFaceUp,
                    isMatched = savedCard.isMatched
                )
            }

            _uiState.value = GameUiState(
                cards = loadedCards,
                score = savedState.score,
                selectedCards = emptyList(), // Siempre inicia sin selección
                isCheckingMatch = false
            )
            return true

        } catch (e: Exception) {
            // Si falla el parsing (p.ej. datos corruptos), borra la partida y empieza de nuevo
            prefs.edit().remove(SAVED_GAME_KEY).apply()
            return false
        }
    }

}