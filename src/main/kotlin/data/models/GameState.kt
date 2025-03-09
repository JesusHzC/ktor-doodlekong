package com.jesushz.data.models

import com.jesushz.other.Constants.TYPE_GAME_STATE

data class GameState(
    val drawingPlayer: String,
    val word: String,
): BaseModel(TYPE_GAME_STATE)
