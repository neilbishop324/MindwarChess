package com.neilb.mindwarchess.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.neilb.mindwarchess.databinding.MoveRowBinding
import com.neilb.mindwarchess.model.Move
import com.neilb.mindwarchess.model.Move.Companion.enPassant
import com.neilb.mindwarchess.model.Move.Companion.kingSideCastling
import com.neilb.mindwarchess.model.Move.Companion.promotion
import com.neilb.mindwarchess.model.Move.Companion.queenSideCastling
import com.neilb.mindwarchess.model.PieceInPosition.Companion.getPieceById
import com.neilb.mindwarchess.model.PieceInPosition.Companion.pawn

class MoveAdapter(
    private val movesList: ArrayList<Pair<Move, Move?>>
) : RecyclerView.Adapter<MoveAdapter.MoveHolder>() {

    class MoveHolder(val binding: MoveRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveHolder {
        val binding = MoveRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MoveHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MoveHolder, position: Int) {
        val view = holder.binding
        view.moveNumber.text = "${position + 1}."
        view.whiteMove.text = getMoveText(movesList[position].first)
        view.blackMove.text = movesList[position].second?.let { getMoveText(it) }
    }

    private fun getMoveText(move: Move) : String {
        when (move.type) {
            queenSideCastling -> return "O-O-O"
            kingSideCastling -> return "O-O"
        }
        var pieceName = if (move.piece == pawn.id && move.killPiece != null) getXCoordinateName(move.from.second).toString() else getPieceById(move.piece).name
        val differenceInY = if (move.isWhite) 1 else -1
        val coordinateName = if (move.type == enPassant)
            "${if (move.killPiece != null) "x" else ""}${getXCoordinateName(move.to.second)}${getYCoordinateName(move.to.first + differenceInY)}"
        else
            "${if (move.killPiece != null) "x" else ""}${getXCoordinateName(move.to.second)}${getYCoordinateName(move.to.first)}"

        pieceName += coordinateName

        if (move.type == promotion) {
            pieceName += "=${getPieceById(move.promotionPiece!!).name}"
        }
        return pieceName
    }

    private fun getXCoordinateName(pos: Int) = "abcdefgh"[pos]
    private fun getYCoordinateName(pos: Int) = 8 - pos

    override fun getItemCount(): Int = movesList.size
}