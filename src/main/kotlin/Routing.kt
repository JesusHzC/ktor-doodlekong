package com.jesushz

import com.jesushz.routes.createRoomRoutes
import com.jesushz.routes.getRoomsRoutes
import com.jesushz.routes.joinRoomRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        createRoomRoutes()
        getRoomsRoutes()
        joinRoomRoutes()
    }
}
