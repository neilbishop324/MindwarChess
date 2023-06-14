package com.neilb.mindwarchess.model

data class PieceInPosition(
    var status: Int,
    val piece: Int? = null) {

    companion object {
        const val isBlack = 0
        const val isWhite = 1
        const val isBlank = 2
        const val isTarget = 3

        val king = Piece(0, -1, "K")
        val queen = Piece(1, 9, "Q")
        val rook = Piece(2, 5, "R")
        val knight = Piece(3, 3, "N")
        val bishop = Piece(4, 3, "B")
        val pawn = Piece(5, 1, "")

        fun getPieceById(id: Int) : Piece {
            return when (id) {
                0 -> king
                1 -> queen
                2 -> rook
                3 -> knight
                4 -> bishop
                else -> pawn
            }
        }
    }
}