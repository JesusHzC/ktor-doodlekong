package com.jesushz.routes

import com.jesushz.data.Room
import com.jesushz.data.models.BasicApiResponse
import com.jesushz.data.models.CreateRoomRequest
import com.jesushz.data.models.RoomResponse
import com.jesushz.other.Constants.MAX_ROOM_SIZE
import com.jesushz.other.Constants.MIN_ROOM_SIZE
import com.jesushz.server
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.createRoomRoutes() {
    route("/api/createRoom") {
        post {
            val roomRequest = call.receiveNullable<CreateRoomRequest>()
            if (roomRequest == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            if (server.rooms[roomRequest.name] != null) {
                call.respond(
                    HttpStatusCode.OK,
                    BasicApiResponse(false, "Room already exists")
                )
                return@post
            }

            if (roomRequest.maxPlayers < MIN_ROOM_SIZE) {
                call.respond(
                    HttpStatusCode.OK,
                    BasicApiResponse(false, "The minimum number  of players need to be less than $MIN_ROOM_SIZE")
                )
                return@post
            }

            if (roomRequest.maxPlayers > MAX_ROOM_SIZE) {
                call.respond(
                    HttpStatusCode.OK,
                    BasicApiResponse(false, "The maximum room size is $MAX_ROOM_SIZE")
                )
                return@post
            }

            val room = Room(
                name = roomRequest.name,
                maxPlayers = roomRequest.maxPlayers
            )
            server.rooms[roomRequest.name] = room
            println("Room created: ${roomRequest.name}")

            call.respond(HttpStatusCode.Created, BasicApiResponse(true))
        }
    }
}

fun Routing.getRoomsRoutes() {
    route("/api/getRooms") {
        get {
            val searchQuery = call.parameters["searchQuery"]
            if (searchQuery == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }

            val roomsResult = server.rooms.filterKeys { key ->
                key.contains(searchQuery, ignoreCase = true)
            }
            val roomsResponse = roomsResult.values.map { room ->
                RoomResponse(
                    name = room.name,
                    maxPlayers = room.maxPlayers,
                    playerCount = room.players.size
                )
            }.sortedBy { it.name }

            call.respond(HttpStatusCode.OK, roomsResponse)
        }
    }
}