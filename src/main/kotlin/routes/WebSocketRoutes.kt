package com.jesushz.routes

import com.google.gson.JsonParser
import com.jesushz.data.Player
import com.jesushz.data.Room
import com.jesushz.data.models.*
import com.jesushz.gson
import com.jesushz.other.Constants.TYPE_ANNOUNCEMENT
import com.jesushz.other.Constants.TYPE_CHAT_MESSAGE
import com.jesushz.other.Constants.TYPE_CHOSEN_WORD
import com.jesushz.other.Constants.TYPE_DISCONNECT_REQUEST
import com.jesushz.other.Constants.TYPE_DRAW_ACTION
import com.jesushz.other.Constants.TYPE_DRAW_DATA
import com.jesushz.other.Constants.TYPE_GAME_STATE
import com.jesushz.other.Constants.TYPE_JOIN_ROOM_HANDSHAKE
import com.jesushz.other.Constants.TYPE_PHASE_CHANGE
import com.jesushz.other.Constants.TYPE_PING
import com.jesushz.server
import com.jesushz.session.DrawingSession
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach

fun Route.gameWebSocketRoutes() {
    route("/ws/draw") {
        standardWebSocket { socket, clientId, message, payload ->
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
                    val room = server.getRoomWithClientId(clientId)  ?: return@standardWebSocket
                    room.broadcastToAllExcept(message, clientId)
                    room.addSerializedDrawAction(message)
                }
            }
        }
    }
}

internal fun Route.standardWebSocket(
    handleFrame: suspend (
        socket: DefaultWebSocketServerSession,
        clientId: String,
        message: String,
        payload: BaseModel
    ) -> Unit
) {
    webSocket {
        val session = call.sessions.get<DrawingSession>()
        if (session == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
            return@webSocket
        }

        try {
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val message = frame.readText()
                    val jsonObject = JsonParser.parseString(message).asJsonObject
                    val type = when (jsonObject.get("type").asString) {
                        TYPE_CHAT_MESSAGE -> ChatMessage::class.java
                        TYPE_DRAW_DATA -> DrawData::class.java
                        TYPE_ANNOUNCEMENT -> Announcement::class.java
                        TYPE_JOIN_ROOM_HANDSHAKE -> JoinRoomHandshake::class.java
                        TYPE_PHASE_CHANGE -> PhaseChange::class.java
                        TYPE_CHOSEN_WORD -> ChosenWord::class.java
                        TYPE_GAME_STATE -> GameState::class.java
                        TYPE_PING -> Ping::class.java
                        TYPE_DISCONNECT_REQUEST -> DisconnectRequest::class.java
                        TYPE_DRAW_ACTION -> DrawAction::class.java
                        else -> BaseModel::class.java
                    }
                    val payload = gson.fromJson(message, type)
                    handleFrame(
                        this@webSocket,
                        session.clientId,
                        message,
                        payload
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Handle disconnects
            val playerWithClientId = server
                .getRoomWithClientId(session.clientId)?.players?.find { it.clientId == session.clientId }
            if (playerWithClientId != null) {
                server.playerLeft(session.clientId)
            }
        }
    }
}