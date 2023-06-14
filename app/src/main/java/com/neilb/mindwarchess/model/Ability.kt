package com.neilb.mindwarchess.model

class Ability {

    companion object {
        fun getAbilityByPieceId(isWhite: Boolean, pieceId: Int): Ability {
            when (pieceId) {
                PieceInPosition.rook.id -> {
                    return Ability(
                        horizontalMoving = true,
                        verticalMoving = true,
                        crossMoving = false
                    )
                }
                PieceInPosition.knight.id -> {
                    return Ability(
                        limitedPairs =
                        arrayOf(
                            Triple(1, -2, true),
                            Triple(2, -1, true),
                            Triple(1, 2, true),
                            Triple(2, 1, true),
                            Triple(-1, -2, true),
                            Triple(-2, -1, true),
                            Triple(-1, 2, true),
                            Triple(-2, 1, true),
                        )
                    )
                }
                PieceInPosition.bishop.id -> {
                    return Ability(
                        horizontalMoving = false,
                        verticalMoving = false,
                        crossMoving = true
                    )
                }
                PieceInPosition.queen.id -> {
                    return Ability(
                        horizontalMoving = true,
                        verticalMoving = true,
                        crossMoving = true
                    )
                }
                PieceInPosition.king.id -> {
                    return Ability(
                        limitedPairs =
                        arrayOf(
                            Triple(1, -1, true),
                            Triple(1, 0, true),
                            Triple(1, 1, true),
                            Triple(0, 1, true),
                            Triple(0, -1, true),
                            Triple(-1, -1, true),
                            Triple(-1, 0, true),
                            Triple(-1, 1, true),
                            Triple(0, -2, false),
                            Triple(0, 2, false),
                        )
                    )
                }
                else -> {
                    //pawn
                    val destination = if (isWhite) -1 else 1
                    return Ability(
                        limitedPairs = arrayOf(
                            Triple(1 * destination, 0, true),
                            Triple(2 * destination, 0, true),
                            Triple(1 * destination, -1, true),
                            Triple(1 * destination, 1, true),
                        )
                    )
                }
            }
        }
    }

    var horizontalMoving: Boolean? = null
    var verticalMoving: Boolean? = null
    var crossMoving: Boolean? = null

    var limitedPairs: Array<Triple<Int, Int, Boolean>>? = null

    //unlimited moving
    constructor(
        horizontalMoving: Boolean,
        verticalMoving: Boolean,
        crossMoving: Boolean
    ) {
        this.horizontalMoving = horizontalMoving
        this.verticalMoving = verticalMoving
        this.crossMoving = crossMoving
    }

    //limited moving
    constructor(
        limitedPairs: Array<Triple<Int, Int, Boolean>>
    ) {
        this.limitedPairs = limitedPairs
    }

}
