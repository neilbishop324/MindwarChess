package com.neilb.mindwarchess.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.neilb.mindwarchess.model.Move
import com.neilb.mindwarchess.model.PieceInPosition
import com.neilb.mindwarchess.model.PieceInPosition.Companion.getPieceById
import com.neilb.mindwarchess.util.toInt

class GameViewModel : ViewModel() {

    private val _whiteTurn = MutableLiveData(true)
    val whiteTurn : LiveData<Boolean> = _whiteTurn

    fun setWhiteTurn(value: Boolean) {
        _whiteTurn.value = value
    }

    private val _inTarget = MutableLiveData(false)
    val inTarget : LiveData<Boolean> = _inTarget

    fun setInTarget(value: Boolean) {
        _inTarget.value = value
    }

    private val _targetPosition = MutableLiveData<Pair<Int, Int>?>(null)
    val targetPosition : LiveData<Pair<Int, Int>?> = _targetPosition

    fun setTargetPosition(value: Pair<Int, Int>?) {
        _targetPosition.value = value
    }

    private val _isInCheck = MutableLiveData(Pair(false, false))
    val isInCheck : LiveData<Pair<Boolean, Boolean>> = _isInCheck

    fun setIsInCheck(value: Pair<Boolean, Boolean>) {
        _isInCheck.value = value
    }

    private val _kingMoved = MutableLiveData(Pair(false, false))
    val kingMoved : LiveData<Pair<Boolean, Boolean>> = _kingMoved

    fun setKingMoved(value: Pair<Boolean, Boolean>) {
        _kingMoved.value = value
    }

    private val _queenSideRookMoved = MutableLiveData(Pair(false, false))

    fun setQueenSideRookMoved(value: Pair<Boolean, Boolean>) {
        _queenSideRookMoved.value = value
    }

    private val _kingSideRookMoved = MutableLiveData(Pair(false, false))

    fun setKingSideRookMoved(value: Pair<Boolean, Boolean>) {
        _kingSideRookMoved.value = value
    }

    companion object {
        fun calculatePoint(pieces: List<PieceInPosition>, isWhite: Boolean): Int {
            val playerPieces = pieces.filter { it.piece != null && it.status == isWhite.toInt() }
            val points = playerPieces.map { getPieceById(it.piece!!).point }
            return points.sum()
        }

        fun getKillPieces(moves: List<Move>, isWhite: Boolean): List<Int> {
            return moves.filterIndexed { index, move -> index % 2 == isWhite.toInt() && move.killPiece != null }
                .map { it.killPiece!!.piece!! }.sortedByDescending { getPieceById(it).point }
        }
    }

    fun castleCheck1(queenSide: Boolean, isWhite: Boolean): Boolean {
        return if (isWhite)
            !_isInCheck.value!!.first && !_kingMoved.value!!.first &&
                    if (queenSide)
                        !_queenSideRookMoved.value!!.first
                    else !_kingSideRookMoved.value!!.first
        else !_isInCheck.value!!.second && !_kingMoved.value!!.second &&
                if (queenSide)
                    !_queenSideRookMoved.value!!.second
                else
                    !_kingSideRookMoved.value!!.second
    }

    fun playAgain() {
        setWhiteTurn(true)
        setInTarget(false)
        setTargetPosition(null)
        setIsInCheck(false to false)
        setKingMoved(false to false)
        setQueenSideRookMoved(false to false)
        setKingSideRookMoved(false to false)
    }

}