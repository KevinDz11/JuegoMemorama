// ruta: app/src/main/java/com/example/juegoks_memorama/ui/screens/MemoryGameScreen.kt
package com.example.juegoks_memorama.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.juegoks_memorama.viewmodel.MemoryGameViewModel
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import com.example.juegoks_memorama.model.Difficulty
import com.example.juegoks_memorama.model.GameMode
import com.example.juegoks_memorama.model.SaveFormat
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.juegoks_memorama.model.GameHistoryItem
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
// --- IMPORTS AÑADIDOS ---
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.width

@Composable
private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

// --- NUEVO: Función para formatear fecha y hora ---
@Composable
private fun formatTimestamp(timestamp: Long): String {
    val sdf = remember { SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) }
    return sdf.format(Date(timestamp))
}

@Composable
fun MemoryGameScreen(
    gameMode: GameMode,
    initialDifficulty: Difficulty, // Nuevo parámetro
    onExitGame: () -> Unit,
    viewModel: MemoryGameViewModel = hiltViewModel()
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val uiState by viewModel.gameUiState.collectAsStateWithLifecycle() // Nuevo estado UI
    val scope = rememberCoroutineScope() // Necesario para efectos visuales/sonoros (si se implementan)

    // Nota: La implementación de retroalimentación sonora con MediaPlayer requiere
    // añadir el archivo de sonido a res/raw y usar el contexto. Aquí se mantiene como mockup.
    val playMatchSound = {}
    val playNoMatchSound = {}

    // Detección de match para feedback visual/sonoro
    LaunchedEffect(gameState.matchedPairs, gameState.moves) {
        if (gameState.moves > 0) {
            // Si la racha es > 0, es un match
            if (gameState.matchStreak > 0) {
                // playMatchSound()
            } else if (gameState.cards.none { it.isFaceUp && !it.isMatched }) {
                // Si no hay cartas volteadas y la racha es 0, implica un no-match reciente
                // playNoMatchSound()
            }
        }
    }


    LaunchedEffect(initialDifficulty) {
        // Asegura que el ViewModel cargue la dificultad seleccionada
        viewModel.setDifficulty(initialDifficulty)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // <-- REQ 4: Añadir padding para la barra de estado
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (gameMode == GameMode.SINGLE_PLAYER) "Modo: Un Jugador (${gameState.difficulty.name})" else "Modo: Multijugador (Bluetooth)",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GameHeader(
            moves = gameState.moves,
            matchedPairs = gameState.matchedPairs,
            score = gameState.score, // Pasar la puntuación
            elapsedTime = gameState.elapsedTimeInSeconds,
            maxPairs = gameState.difficulty.pairs, // Pasar el máximo de pares
            onNewGame = { viewModel.startNewGame() },
            onExitGame = onExitGame,
            onSaveClick = { viewModel.onSaveClick() }, // <-- CAMBIO: Llama a onSaveClick
            onShowHistoryDialog = { viewModel.showHistoryDialog(true) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (gameMode == GameMode.BLUETOOTH) {
            Text(
                text = "Lógica de conexión Bluetooth en desarrollo...",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        CardGrid(
            cards = gameState.cards,
            columns = gameState.difficulty.columns, // Usar columnas de la dificultad
            onCardClick = { card -> viewModel.onCardClick(card) },
            modifier = Modifier.weight(1f) // NUEVO: Para responsividad
        )

        if (gameState.gameCompleted) {
            GameCompletedDialog(
                moves = gameState.moves,
                score = gameState.score, // Mostrar puntuación final
                onPlayAgain = { viewModel.startNewGame() },
                onSaveResult = { viewModel.onSaveClick() }, // <-- CAMBIO: Llama a onSaveClick
                onExit = onExitGame
            )
        }

        // --- DIÁLOGOS DE GUARDADO/CARGA ---
        if (uiState.showSaveDialog) {
            SaveGameDialog(
                // --- NUEVO: Pasar la lista de nombres para validación ---
                existingSaveNames = uiState.existingSaveNames,
                onSave = { filename, format -> // CAMBIO
                    scope.launch { viewModel.saveGame(filename, format) } // CAMBIO
                },
                onDismiss = { viewModel.showSaveDialog(false) }
            )
        }

        // CAMBIO: Cargar el nuevo Diálogo de Historial
        if (uiState.showHistoryDialog) {
            HistoryDialog(
                historyItems = uiState.historyItems,
                onLoad = { filename, format ->
                    scope.launch { viewModel.loadGameFromHistory(filename, format) }
                },
                onDismiss = { viewModel.showHistoryDialog(false) }
            )
        }

        // --- REQ 1 & 2: Diálogo Post-Guardado (En curso) ---
        if (uiState.showPostSaveDialog) {
            PostSaveDialog(
                onContinue = { viewModel.dismissPostSaveDialog() },
                onNewGame = {
                    viewModel.dismissPostSaveDialog()
                    viewModel.startNewGame()
                },
                onExit = {
                    viewModel.dismissPostSaveDialog()
                    onExitGame()
                }
            )
        }

        // --- REQ 7: Diálogo Post-Guardado (Partida Ganada) ---
        if (uiState.showPostWinSaveDialog) {
            PostWinSaveDialog(
                onNewGame = {
                    viewModel.dismissPostWinSaveDialog()
                    viewModel.startNewGame()
                },
                onExit = {
                    viewModel.dismissPostWinSaveDialog()
                    onExitGame()
                }
            )
        }
    }
}

// --- NUEVO: Helper Composable para las estadísticas ---
@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp) // Añadir padding
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Color más sutil
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GameHeader(
    moves: Int,
    matchedPairs: Int,
    score: Int,
    elapsedTime: Long,
    maxPairs: Int,
    onNewGame: () -> Unit,
    onExitGame: () -> Unit,
    onSaveClick: () -> Unit,
    onShowHistoryDialog: () -> Unit
) {
    // --- REQ: Layout de Header rediseñado (Arreglo) ---
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Fila de Estadísticas ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround // Distribuye el espacio
        ) {
            StatItem(label = "Puntuación", value = "$score")
            StatItem(label = "Movimientos", value = "$moves")
            StatItem(label = "Parejas", value = "$matchedPairs/$maxPairs")
            StatItem(label = "Tiempo", value = formatTime(elapsedTime))
        }

        Spacer(Modifier.height(16.dp)) // Espacio entre stats y botones

        // --- Fila 1 de Botones ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            Button(onClick = onNewGame) { Text("🔄 Nuevo") }
            Button(onClick = onSaveClick) { Text("💾 Guardar") }
        }

        Spacer(Modifier.height(8.dp)) // Espacio entre filas de botones

        // --- Fila 2 de Botones ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            Button(onClick = onShowHistoryDialog) { Text("📜 Historial") }
            Button(onClick = onExitGame) { Text("🚪 Salir") }
        }
    }
}


@Composable
fun CardGrid(
    cards: List<com.example.juegoks_memorama.model.Card>,
    columns: Int, // Nuevo parámetro
    onCardClick: (com.example.juegoks_memorama.model.Card) -> Unit,
    modifier: Modifier = Modifier // AÑADE ESTE PARÁMETRO
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns), // Usar columnas dinámicamente
        modifier = modifier.fillMaxWidth(), // APLICA EL MODIFICADOR AQUÍ
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cards) { card ->
            MemoryCard(
                card = card,
                onClick = { onCardClick(card) }
            )
        }
    }
}

@Composable
fun MemoryCard(
    card: com.example.juegoks_memorama.model.Card,
    onClick: () -> Unit
) {
    val rotation = remember { Animatable(0f) }
    var isFaceUp by remember { mutableStateOf(card.isFaceUp) }

    LaunchedEffect(card.isFaceUp) {
        if (card.isFaceUp != isFaceUp) {
            // Animación de rotación para voltear
            if (card.isFaceUp) {
                rotation.animateTo(180f, animationSpec = tween(500))
            } else {
                rotation.animateTo(0f, animationSpec = tween(500))
            }
            isFaceUp = card.isFaceUp
        }
    }

    // --- EFECTO VISUAL: Escala a 0 para "desaparecer" al matchear ---
    val animateScale by animateFloatAsState(
        targetValue = if (card.isMatched) 0.0f else 1f, // Escala a 0 al matchear
        animationSpec = if (card.isMatched) {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        },
        label = "scale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(0.75f)
            .scale(animateScale) // Aplicar el efecto de escala
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    enabled = !card.isMatched && !card.isFaceUp,
                    onClick = onClick
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (rotation.value > 90f) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        ) {
            if (rotation.value > 90f) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.value.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QuestionMark,
                        contentDescription = "Carta volteada",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GameCompletedDialog(
    moves: Int,
    score: Int,
    onPlayAgain: () -> Unit,
    onSaveResult: () -> Unit, // <-- CAMBIO: Renombrado
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onPlayAgain() }, // Jugar de nuevo si toca fuera
        title = { Text("¡Felicidades!") },
        text = {
            Column {
                Text("Completaste el juego en $moves movimientos.")
                Text("Puntuación Final: $score puntos.")
            }
        },
        // --- REQ 6: Layout de diálogo de victoria corregido ---
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onSaveResult) { // CAMBIO
                    Text("💾 Guardar Resultado")
                }
                Button(onClick = onPlayAgain) {
                    Text("🔄 Jugar de nuevo")
                }
                Button(onClick = onExit) { // NUEVO
                    Text("🚪 Salir")
                }
            }
        },
        dismissButton = { } // Dejar vacío
    )
}

// --- MODIFICADO: Diálogo para Guardar Partida ---
@Composable
fun SaveGameDialog(
    existingSaveNames: List<String>, // <-- NUEVO: Para validación
    onSave: (String, SaveFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(SaveFormat.JSON) }
    var filename by rememberSaveable { mutableStateOf("") }

    // --- REQ: Lógica de validación ---
    val isBlank = filename.isBlank()
    // Comprueba si el nombre (ignorando may/min) ya existe en la lista
    val isDuplicate = existingSaveNames.any { it.equals(filename, ignoreCase = true) }

    val isError = isBlank || isDuplicate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar Partida") },
        text = {
            Column {
                Text("Ingresa un nombre para tu partida:")
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text("Nombre de archivo") },
                    singleLine = true,
                    isError = isError // <-- Aplicar estado de error
                )

                // --- REQ: Mostrar mensaje de error específico ---
                if (isBlank) {
                    Text(
                        "El nombre no puede estar vacío",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (isDuplicate) {
                    Text(
                        "Ese nombre de archivo ya existe",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Selecciona el formato de guardado:")
                Spacer(modifier = Modifier.height(16.dp))

                SaveFormat.entries.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = format }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${if (selectedFormat == format) "●" else "○"} ${format.name} (.${format.name.lowercase()})")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                // Deshabilitado si hay error (vacío O duplicado)
                onClick = { onSave(filename, selectedFormat) },
                enabled = !isError
            ) { Text("Guardar") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}


// --- NUEVO: Diálogo para Historial (Reemplaza a LoadGameDialog) ---
@Composable
fun HistoryDialog(
    historyItems: List<GameHistoryItem>,
    onLoad: (String, SaveFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFile by remember { mutableStateOf<Pair<String, SaveFormat>?>(null) }

    // Particiona la lista en dos: en curso y completadas
    val (completed, inProgress) = historyItems.partition { it.state.gameCompleted }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historial de Partidas") },
        text = {
            // Usamos LazyColumn para listas largas
            LazyColumn(modifier = Modifier.fillMaxWidth()) {

                // --- SECCIÓN: EN CURSO ---
                item {
                    Text(
                        text = "Partidas en Curso",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                if (inProgress.isEmpty()) {
                    item { Text("No hay partidas guardadas en curso.") }
                } else {
                    items(inProgress) { item ->
                        HistoryItemRow(
                            item = item,
                            isFinished = false,
                            isSelected = selectedFile?.first == item.filename,
                            onSelect = { selectedFile = item.filename to item.format }
                        )
                    }
                }

                // --- SECCIÓN: COMPLETADAS ---
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = "Partidas Completadas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                if (completed.isEmpty()) {
                    item { Text("No hay partidas completadas.") }
                } else {
                    items(completed) { item ->
                        HistoryItemRow(
                            item = item,
                            isFinished = true,
                            isSelected = false, // No se pueden seleccionar
                            onSelect = {} // No hacer nada
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedFile?.let { onLoad(it.first, it.second) } },
                enabled = selectedFile != null // Solo habilitado si se selecciona una partida en curso
            ) { Text("Cargar") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun HistoryItemRow(
    item: GameHistoryItem,
    isFinished: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    // --- REQ 3: Añadir timestamp ---
    // 'filename' aquí SÍ tiene extensión
    val (filename, _, state, timestamp) = item
    val formattedTimestamp = formatTimestamp(timestamp)

    val modifier = if (isFinished) {
        Modifier.padding(vertical = 8.dp) // No clickeable
    } else {
        Modifier
            .clickable { onSelect() }
            .padding(vertical = 8.dp)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isFinished) {
            // Radio button para seleccionar
            Text(
                text = if (isSelected) "●" else "○",
                modifier = Modifier.padding(end = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            // Mostramos el nombre de archivo SIN extensión para que sea más limpio
            Text(filename.substringBeforeLast('.'), fontWeight = FontWeight.SemiBold)
            // --- REQ 3: Mostrar fecha y hora ---
            Text(
                text = formattedTimestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isFinished) {
                // Mostrar estadísticas de partidas terminadas
                Text(
                    text = "Score: ${state.score} | Mov: ${state.moves} | Tiempo: ${formatTime(state.elapsedTimeInSeconds)}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                // Mostrar progreso de partidas en curso
                Text(
                    text = "Progreso: ${state.matchedPairs}/${state.difficulty.pairs} pares | Mov: ${state.moves}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// --- REQ 1 & 2: Diálogo después de guardar (en curso) ---
@Composable
fun PostSaveDialog(
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onContinue, // Continuar si toca fuera
        title = { Text("Partida Guardada") },
        text = { Text("¿Qué deseas hacer ahora?") },
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onContinue) {
                    Text("Continuar partida")
                }
                Button(onClick = onNewGame) {
                    Text("🔄 Nuevo juego")
                }
                TextButton(onClick = onExit) {
                    Text("🚪 Salir al menú")
                }
            }
        },
        dismissButton = {}
    )
}

// --- REQ 7: Diálogo después de guardar (partida ganada) ---
@Composable
fun PostWinSaveDialog(
    onNewGame: () -> Unit,
    onExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onNewGame, // Jugar de nuevo si toca fuera
        title = { Text("Resultado Guardado") },
        text = { Text("¿Qué deseas hacer ahora?") },
        confirmButton = {
            Button(onClick = onNewGame) {
                Text("🔄 Jugar de nuevo")
            }
        },
        dismissButton = {
            TextButton(onClick = onExit) {
                Text("🚪 Salir al menú")
            }
        }
    )
}