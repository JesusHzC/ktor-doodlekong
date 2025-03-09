package com.jesushz.data.models

import com.jesushz.other.Constants.TYPE_NEW_WORDS
data class NewWords(
    val newWords: List<String>
): BaseModel(TYPE_NEW_WORDS)
