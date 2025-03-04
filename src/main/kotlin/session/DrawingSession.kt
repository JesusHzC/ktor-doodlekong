package com.jesushz.session

import kotlinx.serialization.Serializable

@Serializable
data class DrawingSession(
    val clientId: String,
    val sessionId: String,
)
