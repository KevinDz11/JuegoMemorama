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
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
@Composable
fun MemoryGameScreen(
    viewModel: MemoryGameViewModel = hiltViewModel()
) {
    // CORRECCIÓN: Cambiar collectAsState() por collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameHeader(
            moves = gameState.moves,
            matchedPairs = gameState.matchedPairs,
            elapsedTime = gameState.elapsedTimeInSeconds,
            onNewGame = { viewModel.startNewGame() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        CardGrid(
            cards = gameState.cards,
            onCardClick = { card -> viewModel.onCardClick(card) }
        )

        if (gameState.gameCompleted) {
            GameCompletedDialog(
                moves = gameState.moves,
                onPlayAgain = { viewModel.startNewGame() }
            )
        }
    }
}

@Composable
fun GameHeader(
    moves: Int,
    matchedPairs: Int,
    elapsedTime: Long,
    onNewGame: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Movimientos: $moves",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Parejas: $matchedPairs/15",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Tiempo: ${formatTime(elapsedTime)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(onClick = onNewGame) {
            Text("Nuevo Juego")
        }
    }
}

@Composable
fun CardGrid(
    cards: List<com.example.juegoks_memorama.model.Card>,
    onCardClick: (com.example.juegoks_memorama.model.Card) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        modifier = Modifier.fillMaxWidth(),
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
    val scope = rememberCoroutineScope()

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
        targetValue = if (card.isMatched) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
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
    onPlayAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("¡Felicidades!") },
        text = { Text("Completaste el juego en $moves movimientos") },
        confirmButton = {
            Button(onClick = onPlayAgain) {
                Text("Jugar de nuevo")
            }
        }
    )
}