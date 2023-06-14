package com.neilb.mindwarchess.game

class Game {
    var whiteTurn = true
    var inTarget = false
    var targetPosition: Pair<Int, Int>? = null

    var isInCheck = Pair(false, false)
    var kingMoved = Pair(false, false)
    var queenSideRookMoved = Pair(false, false)
    var kingSideRookMoved = Pair(false, false)

    fun castleCheck1(queenSide: Boolean, isWhite: Boolean): Boolean {
        return if (isWhite)
            !isInCheck.first && !kingMoved.first &&
                    if (queenSide)
                        !queenSideRookMoved.first
                    else !kingSideRookMoved.first
            else !isInCheck.second && !kingMoved.second &&
                    if (queenSide)
                        !queenSideRookMoved.second
                    else
                        !kingSideRookMoved.second
    }

}