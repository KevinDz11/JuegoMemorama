package com.example.juegoks_memorama.ui2 // Tu paquete ui2

// Importaciones necesarias para la UI y la animación
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
// --- CORRECCIÓN AQUÍ ---
import androidx.compose.material3.icons.Icons // Importación de Material 3
import androidx.compose.material3.icons.filled.QuestionMark // Importación de Material 3
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.juegoks_memorama.MemoryCard // Importa tu modelo

@Composable
fun MemoryCardItem(
    card: MemoryCard,
    onClick: () -> Unit
) {
    // Esta es la variable que animará la rotación
    val rotation by animateFloatAsState(
        targetValue = if (card.isFaceUp) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardRotation"
    )

    Card(
        onClick = { if (!card.isFaceUp && !card.isMatched) onClick() },
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f) // Para que sea cuadrada
            .graphicsLayer {
                // Aplica la rotación en el eje Y (volteo)
                rotationY = rotation
                cameraDistance = 8 * density
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (card.isMatched) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Si la rotación pasó los 90 grados, muestra el frente (el icono)
            if (rotation > 90f) {
                Icon(
                    imageVector = card.imageVector,
                    contentDescription = "Icono de la carta",
                    modifier = Modifier
                        .fillMaxSize(0.8f)
                        .graphicsLayer {
                            // Voltea el icono para que no se vea espejado
                            rotationY = 180f
                        }
                )
            } else {
                // Si no, muestra el reverso (el signo de interrogación)
                Icon(
                    // ¡AQUÍ SE USA!
                    imageVector = Icons.Filled.QuestionMark,
                    contentDescription = "Reverso de la carta",
                    modifier = Modifier.fillMaxSize(0.8f)
                )
            }
        }
    }
}