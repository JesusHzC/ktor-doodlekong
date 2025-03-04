package com.jesushz

import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

val server = DrawingServer()

fun Application.module() {
    configureSerialization()
    configureSockets()
    configureMonitoring()
    configureRouting()
    configureSession()
}
