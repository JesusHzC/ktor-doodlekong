package com.jesushz

import com.jesushz.routes.createRoomRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        createRoomRoutes()
    }
}
