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
import com.example.juegoks_memorama.model.SaveFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Configuración de DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_settings")

@Singleton
class GameRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gameStateKey = stringPreferencesKey("game_state")

    // Flujo para leer el GameState (Guardado automático JSON/DataStore)
    val savedGameState: Flow<GameState?> = context.dataStore.data
        .map { preferences ->
            preferences[gameStateKey]?.let { jsonString ->
                try {
                    // CORRECCIÓN: Si falla la deserialización debido a una estructura antigua,
                    // esta función simplemente retorna null.
                    Json.decodeFromString<GameState>(jsonString)
                } catch (e: Exception) {
                    null // Retorna null si hay error de deserialización
                }
            }
        }

    // Función para guardar el GameState (Guardado automático JSON/DataStore)
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

    // --- LÓGICA DE GUARDADO MANUAL MULTI-FORMATO (Faltante en tu archivo) ---
    private fun getFileExtension(format: SaveFormat) = when (format) {
        SaveFormat.JSON -> ".json"
        SaveFormat.XML -> ".xml"
        SaveFormat.TXT -> ".txt"
    }

    // Guardado manual
    suspend fun saveGameManual(gameState: GameState, format: SaveFormat, filename: String = "memorama_save") = withContext(Dispatchers.IO) {
        val extension = getFileExtension(format)
        val file = File(context.filesDir, "$filename$extension")
        val data = when (format) {
            SaveFormat.JSON -> SaveFormatSerializer.serializeToJson(gameState)
            SaveFormat.XML -> SaveFormatSerializer.serializeToXml(gameState)
            SaveFormat.TXT -> SaveFormatSerializer.serializeToTxt(gameState)
        }
        file.writeText(data)
    }

    // Carga manual
    suspend fun loadGameManual(filename: String, format: SaveFormat): GameState? = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, filename)

        if (!file.exists()) return@withContext null

        return@withContext try {
            val content = file.readText()
            when (format) {
                SaveFormat.JSON -> SaveFormatSerializer.deserializeFromJson(content)
                SaveFormat.XML -> SaveFormatSerializer.deserializeFromXml(content)
                SaveFormat.TXT -> SaveFormatSerializer.deserializeFromTxt(content)
            }
        } catch (e: Exception) {
            null
        }
    }

    // Obtener archivos de guardado disponibles
    fun getAvailableSaveFiles(): List<Pair<String, SaveFormat>> {
        return context.filesDir.listFiles { _, name ->
            name.endsWith(".json") || name.endsWith(".xml") || name.endsWith(".txt")
        }?.map { file ->
            val format = when {
                file.name.endsWith(".json") -> SaveFormat.JSON
                file.name.endsWith(".xml") -> SaveFormat.XML
                file.name.endsWith(".txt") -> SaveFormat.TXT
                else -> throw IllegalStateException("Formato de guardado desconocido")
            }
            file.name to format
        }?.sortedByDescending { it.first } ?: emptyList()
    }
}