package com.example.juegoks_memorama.data

import com.example.juegoks_memorama.model.Difficulty
import com.example.juegoks_memorama.model.GameState
import com.example.juegoks_memorama.model.Move
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * Clase utilitaria para serializar y deserializar GameState en diferentes formatos.
 */
object SaveFormatSerializer {

    // --- JSON Serialization ---
    fun serializeToJson(state: GameState): String {
        return Json.encodeToString(state)
    }
    fun deserializeFromJson(jsonString: String): GameState {
        return Json.decodeFromString<GameState>(jsonString)
    }

    // --- XML Serialization (Implementación básica de guardado) ---
    fun serializeToXml(state: GameState): String {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.newDocument()
        val rootElement = doc.createElement("GameState")
        doc.appendChild(rootElement)

        fun appendElement(name: String, value: Any) {
            val element = doc.createElement(name)
            element.appendChild(doc.createTextNode(value.toString()))
            rootElement.appendChild(element)
        }

        // Campos principales
        appendElement("difficulty", state.difficulty.name)
        appendElement("moves", state.moves)
        appendElement("matchedPairs", state.matchedPairs)
        appendElement("score", state.score)
        appendElement("matchStreak", state.matchStreak)
        appendElement("elapsedTimeInSeconds", state.elapsedTimeInSeconds)
        appendElement("gameCompleted", state.gameCompleted)

        // Cartas: Simplificación de la serialización para XML
        state.cards.forEach { card ->
            val cardElement = doc.createElement("Card")
            cardElement.setAttribute("id", card.id.toString())
            cardElement.setAttribute("value", card.value.toString())
            cardElement.setAttribute("isFaceUp", card.isFaceUp.toString())
            cardElement.setAttribute("isMatched", card.isMatched.toString())
            rootElement.appendChild(cardElement)
        }

        // Historial de movimientos
        state.moveHistory.forEach { move -> // MODIFICADO: Usar Move
            val moveElement = doc.createElement("Move")
            moveElement.setAttribute("card1Id", move.card1Id.toString())
            moveElement.setAttribute("card2Id", move.card2Id.toString())
            rootElement.appendChild(moveElement)
        }

        // Serializar a String XML
        val transformer = TransformerFactory.newInstance().newTransformer()
        val writer = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))
        return writer.toString()
    }

    // Deserialización XML se mantiene como no implementada para evitar dependencias complejas de parsing.
    fun deserializeFromXml(xmlString: String): GameState? {
        // CORREGIDO: Devolver null para evitar crash, ya que la carga no está implementada
        return null
    }

    // --- TXT Serialization (key=value format) ---
    fun serializeToTxt(state: GameState): String {
        val cardsString = state.cards.joinToString("|") { "${it.id},${it.value},${it.isFaceUp},${it.isMatched}" }
        val historyString = state.moveHistory.joinToString("|") { "${it.card1Id},${it.card2Id}" }

        return """
            difficulty=${state.difficulty.name}
            moves=${state.moves}
            matchedPairs=${state.matchedPairs}
            score=${state.score}
            matchStreak=${state.matchStreak}
            elapsedTimeInSeconds=${state.elapsedTimeInSeconds}
            gameCompleted=${state.gameCompleted}
            cards=$cardsString
            moveHistory=$historyString
        """.trimIndent()
    }

    fun deserializeFromTxt(txtString: String): GameState {
        val map = txtString.lines().mapNotNull { line ->
            val parts = line.split("=", limit = 2)
            if (parts.size == 2) parts[0].trim() to parts[1].trim() else null
        }.toMap()

        // Lógica de reconstrucción de GameState
        val cards = map["cards"].orEmpty().split("|").map { cardData ->
            val parts = cardData.split(",")
            if (parts.size == 4) com.example.juegoks_memorama.model.Card(
                id = parts[0].toIntOrNull() ?: 0,
                value = parts[1].toIntOrNull() ?: 0,
                isFaceUp = parts[2].toBooleanStrictOrNull() ?: false,
                isMatched = parts[3].toBooleanStrictOrNull() ?: false
            ) else com.example.juegoks_memorama.model.Card(0, 0)
        }.filter { it.value != 0 }

        val history = map["moveHistory"].orEmpty().split("|").mapNotNull { historyEntry ->
            val parts = historyEntry.split(",")
            if (parts.size == 2) Move( // CORREGIDO: Deserializar a Move
                card1Id = parts[0].toIntOrNull() ?: return@mapNotNull null,
                card2Id = parts[1].toIntOrNull() ?: return@mapNotNull null
            )
            else null
        }.filter { it.card1Id != 0 }


        return GameState(
            difficulty = Difficulty.valueOf(map["difficulty"] ?: "MEDIUM"),
            moves = map["moves"]?.toIntOrNull() ?: 0,
            matchedPairs = map["matchedPairs"]?.toIntOrNull() ?: 0,
            score = map["score"]?.toIntOrNull() ?: 0,
            matchStreak = map["matchStreak"]?.toIntOrNull() ?: 0,
            elapsedTimeInSeconds = map["elapsedTimeInSeconds"]?.toLongOrNull() ?: 0,
            gameCompleted = map["gameCompleted"]?.toBooleanStrictOrNull() ?: false,
            cards = cards,
            moveHistory = history
        )
    }
}