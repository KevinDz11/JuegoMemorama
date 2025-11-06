package com.example.juegoks_memorama.ui.screens

import android.view.SoundEffectConstants // <-- AÑADIR
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalView // <-- AÑADIR

@Composable
private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

@Composable
private fun formatTimestamp(timestamp: Long): String {
    val sdf = remember { SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) }
    return sdf.format(Date(timestamp))
}

@Composable
fun MemoryGameScreen(
    gameMode: GameMode,
    initialDifficulty: Difficulty,
    onExitGame: () -> Unit,
    viewModel: MemoryGameViewModel = hiltViewModel()
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val uiState by viewModel.gameUiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialDifficulty) {
        viewModel.setDifficulty(initialDifficulty)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
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
            score = gameState.score,
            elapsedTime = gameState.elapsedTimeInSeconds,
            maxPairs = gameState.difficulty.pairs,
            onNewGame = { viewModel.startNewGame() },
            onExitGame = onExitGame,
            onSaveClick = { viewModel.onSaveClick() },
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
        //Spacer(modifier = Modifier.height(16.dp)) // <- QUITAR ESTE SPACER

        CardGrid(
            cards = gameState.cards,
            columns = gameState.difficulty.columns,
            onCardClick = { card -> viewModel.onCardClick(card) },

            // --- REQ 1: ESTA ES LA SOLUCIÓN AL SCROLL ---
            // Le dice al grid que ocupe todo el espacio sobrante
            modifier = Modifier.weight(1f)
        )

        if (gameState.gameCompleted) {
            GameCompletedDialog(
                moves = gameState.moves,
                score = gameState.score,
                onPlayAgain = { viewModel.startNewGame() },
                onSaveResult = { viewModel.onSaveClick() },
                onExit = onExitGame
            )
        }

        if (uiState.showSaveDialog) {
            SaveGameDialog(
                existingSaveNames = uiState.existingSaveNames,
                onSave = { filename, format ->
                    scope.launch { viewModel.saveGame(filename, format) }
                },
                onDismiss = { viewModel.showSaveDialog(false) }
            )
        }

        if (uiState.showHistoryDialog) {
            HistoryDialog(
                historyItems = uiState.historyItems,
                onLoad = { filename, format ->
                    scope.launch { viewModel.loadGameFromHistory(filename, format) }
                },
                onDismiss = { viewModel.showHistoryDialog(false) }
            )
        }

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

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
    // --- AÑADIR: Obtener la vista actual para reproducir sonidos ---
    val view = LocalView.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Fila de Estadísticas ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(label = "Puntuación", value = "$score")
            StatItem(label = "Movimientos", value = "$moves")
            StatItem(label = "Parejas", value = "$matchedPairs/$maxPairs")
            StatItem(label = "Tiempo", value = formatTime(elapsedTime))
        }

        Spacer(Modifier.height(16.dp))

        // --- Fila 1 de Botones ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            Button(onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK) // <-- AÑADIR SONIDO
                onNewGame()
            }) { Text("🔄 Nuevo") }

            Button(onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK) // <-- AÑADIR SONIDO
                onSaveClick()
            }) { Text("💾 Guardar") }
        }

        Spacer(Modifier.height(8.dp))

        // --- Fila 2 de Botones ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            Button(onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK) // <-- AÑADIR SONIDO
                onShowHistoryDialog()
            }) { Text("📜 Historial") }

            Button(onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK) // <-- AÑADIR SONIDO
                onExitGame()
            }) { Text("🚪 Salir") }
        }
    }
}


@Composable
fun CardGrid(
    cards: List<com.example.juegoks_memorama.model.Card>,
    columns: Int,
    onCardClick: (com.example.juegoks_memorama.model.Card) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxWidth(), // <- El 'modifier' (con weight) se aplica aquí
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
            if (card.isFaceUp) {
                rotation.animateTo(180f, animationSpec = tween(500))
            } else {
                rotation.animateTo(0f, animationSpec = tween(500))
            }
            isFaceUp = card.isFaceUp
        }
    }

    val animateScale by animateFloatAsState(
        targetValue = if (card.isMatched) 0.0f else 1f,
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
            .scale(animateScale)
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
    onSaveResult: () -> Unit,
    onExit: () -> Unit
) {
    // --- AÑADIR: Vista para sonidos de botones ---
    val view = LocalView.current

    AlertDialog(
        onDismissRequest = { onPlayAgain() },
        title = { Text("¡Felicidades!") },
        text = {
            Column {
                Text("Completaste el juego en $moves movimientos.")
                Text("Puntuación Final: $score puntos.")
            }
        },
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onSaveResult()
                }) {
                    Text("💾 Guardar Resultado")
                }
                Button(onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onPlayAgain()
                }) {
                    Text("🔄 Jugar de nuevo")
                }
                Button(onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    onExit()
                }) {
                    Text("🚪 Salir")
                }
            }
        },
        dismissButton = { }
    )
}

@Composable
fun SaveGameDialog(
    existingSaveNames: List<String>,
    onSave: (String, SaveFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(SaveFormat.JSON) }
    var filename by rememberSaveable { mutableStateOf("") }

    val isBlank = filename.isBlank()
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
                    isError = isError
                )

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
                onClick = { onSave(filename, selectedFormat) },
                enabled = !isError
            ) { Text("Guardar") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}


@Composable
fun HistoryDialog(
    historyItems: List<GameHistoryItem>,
    onLoad: (String, SaveFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFile by remember { mutableStateOf<Pair<String, SaveFormat>?>(null) }
    val (completed, inProgress) = historyItems.partition { it.state.gameCompleted }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historial de Partidas") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
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
                            isSelected = false,
                            onSelect = {}
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedFile?.let { onLoad(it.first, it.second) } },
                enabled = selectedFile != null
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
    val (filename, _, state, timestamp) = item
    val formattedTimestamp = formatTimestamp(timestamp)

    val modifier = if (isFinished) {
        Modifier.padding(vertical = 8.dp)
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
            Text(
                text = if (isSelected) "●" else "○",
                modifier = Modifier.padding(end = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(filename.substringBeforeLast('.'), fontWeight = FontWeight.SemiBold)
            Text(
                text = formattedTimestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isFinished) {
                Text(
                    text = "Score: ${state.score} | Mov: ${state.moves} | Tiempo: ${formatTime(state.elapsedTimeInSeconds)}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "Progreso: ${state.matchedPairs}/${state.difficulty.pairs} pares | Mov: ${state.moves}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun PostSaveDialog(
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    onExit: () -> Unit
) {
    val view = LocalView.current // <-- AÑADIR VISTA

    AlertDialog(
        onDismissRequest = onContinue,
        title = { Text("Partida Guardada") },
        text = { Text("¿Qué deseas hacer ahora?") },
        confirmButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK) // <-- AÑADIR SONIDO
                    onContinue()
                }) {
                    Text("Continuar partida")
                }
                Button(onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK) // <-- AÑADIR SONIDO
                    onNewGame()
                }) {
                    Text("🔄 Nuevo juego")
                }
                TextButton(onClick = {
                    view.playSoundEffect(SoundEffectConstants.CLICK) // <-- AÑADIR SONIDO
                    onExit()
                }) {
                    Text("🚪 Salir al menú")
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
fun PostWinSaveDialog(
    onNewGame: () -> Unit,
    onExit: () -> Unit
) {
    val view = LocalView.current // <-- AÑADIR VISTA

    AlertDialog(
        onDismissRequest = onNewGame,
        title = { Text("Resultado Guardado") },
        text = { Text("¿Qué deseas hacer ahora?") },
        confirmButton = {
            Button(onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK) // <-- AÑADIR SONIDO
                onNewGame()
            }) {
                Text("🔄 Jugar de nuevo")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                view.playSoundEffect(SoundEffectConstants.CLICK) // <-- AÑADIR SONIDO
                onExit()
            }) {
                Text("🚪 Salir al menú")
            }
        }
    )
}