package com.neilb.mindwarchess.model

data class Move(
    val id: String,
    val from: Pair<Int, Int>,
    val to: Pair<Int, Int>,
    val piece: Int,
    val isWhite: Boolean,
    var killPiece: PieceInPosition?,
    val type: Int,
    val promotionPiece: Int? = null,
) {
    companion object {
        const val normal = 1
        const val queenSideCastling = 2
        const val kingSideCastling = 3
        const val promotion = 4
        const val enPassant = 5

        const val move = 1
        const val kill = 2
    }
}