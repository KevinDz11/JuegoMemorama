package com.example.juegoks_memorama.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.juegoks_memorama.model.GameState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// Configuración de DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_settings")

@Singleton
class GameRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gameStateKey = stringPreferencesKey("game_state")

    // Flujo para leer el GameState
    val savedGameState: Flow<GameState?> = context.dataStore.data
        .map { preferences ->
            preferences[gameStateKey]?.let { jsonString ->
                try {
                    Json.decodeFromString<GameState>(jsonString)
                } catch (e: Exception) {
                    null // Retorna null si hay error de deserialización
                }
            }
        }

    // Función para guardar el GameState
    suspend fun saveGameState(gameState: GameState) {
        val jsonString = Json.encodeToString(gameState)
        context.dataStore.edit { preferences ->
            preferences[gameStateKey] = jsonString
        }
    }

    // Función para borrar la partida (ej. al iniciar nuevo juego)
    suspend fun clearGameState() {
        context.dataStore.edit { preferences ->
            preferences.remove(gameStateKey)
        }
    }
}