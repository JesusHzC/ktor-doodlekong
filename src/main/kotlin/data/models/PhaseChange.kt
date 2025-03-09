package com.jesushz.data.models

import com.jesushz.data.Room
import com.jesushz.other.Constants.TYPE_PHASE_CHANGE

data class PhaseChange(
    var phase: Room.Phase?,
    var time: Long,
    val drawingPlayer: String? = null,
): BaseModel(TYPE_PHASE_CHANGE)
