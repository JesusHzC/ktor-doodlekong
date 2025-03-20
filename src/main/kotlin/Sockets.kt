package com.jesushz

import com.jesushz.data.Player
import com.jesushz.data.Room
import com.jesushz.data.models.*
import com.jesushz.routes.standardWebSocket
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

fun Application.configureSockets() {
    install(WebSockets)
    routing {
//        webSocket("/ws") { // websocketSession
//            for (frame in incoming) {
//                if (frame is Frame.Text) {
//                    val text = frame.readText()
//                    outgoing.send(Frame.Text("YOU SAID: $text"))
//                    if (text.equals("bye", ignoreCase = true)) {
//                        close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
//                    }
//                }
//            }
//        }
        route("/ws/draw") {
            standardWebSocket { socket, clientId, message, payload ->
                println("socket: $socket, clientId: $clientId, message: $message, payload: $payload")
                when (payload) {
                    is JoinRoomHandshake -> {
                        val room = server.rooms[payload.roomName]
                        if (room == null) {
                            val gameError = GameError(GameError.ERROR_ROOM_NOT_FOUND)
                            socket.send(Frame.Text(gson.toJson(gameError)))
                            return@standardWebSocket
                        }
                        val player = Player(
                            payload.username,
                            socket,
                            payload.clientId
                        )
                        server.playerJoined(player)

                        if (!room.containsPlayer(payload.username)) {
                            room.addPlayer(player.clientId, payload.username, socket)
                        } else {
                            val playerInRoom = room.players.find { it.clientId == clientId }
                            playerInRoom?.socket = socket
                            playerInRoom?.startPinging()
                        }
                    }
                    is DrawData -> {
                        val room = server.rooms[payload.roomName] ?: return@standardWebSocket
                        if (room.phase == Room.Phase.GAME_RUNNING) {
                            room.broadcastToAllExcept(message, clientId)
                            room.addSerializedDrawAction(message)
                        }
                        room.lastDrawData = payload
                    }
                    is ChosenWord -> {
                        val room = server.rooms[payload.roomName] ?: return@standardWebSocket
                        room.setWordAndSwitchToGameRunning(payload.chosenWord)
                    }
                    is ChatMessage -> {
                        val room = server.rooms[payload.roomName] ?: return@standardWebSocket
                        if (!room.checkWordAndNotifyPlayers(payload)) {
                            room.broadcast(message)
                        }
                    }
                    is Ping -> {
                        server.players[clientId]?.receivedPong()
                    }
                    is DisconnectRequest -> {
                        server.playerLeft(clientId, true)
                    }
                    is DrawAction -> {
                        val room = server.getRoomWithClientId(clientId) ?: return@standardWebSocket
                        room.broadcastToAllExcept(message, clientId)
                        room.addSerializedDrawAction(message)
                    }
                }
            }
        }
    }
}
