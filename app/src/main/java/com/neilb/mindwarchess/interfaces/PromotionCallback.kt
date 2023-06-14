package com.neilb.mindwarchess.interfaces

import com.neilb.mindwarchess.model.Move

interface PromotionCallback {
    fun promote(move: Move)
}