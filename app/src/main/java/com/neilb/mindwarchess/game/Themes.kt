package com.neilb.mindwarchess.game

import com.neilb.mindwarchess.R
import com.neilb.mindwarchess.model.PieceInPosition

class PieceThemes {
    companion object {
        fun getTheme1(color: Int, piece: Int): Int {
            return when (piece) {
                PieceInPosition.king.id -> if (color == PieceInPosition.isWhite) R.drawable.t1_w_k else R.drawable.t1_b_k
                PieceInPosition.queen.id -> if (color == PieceInPosition.isWhite) R.drawable.t1_w_q else R.drawable.t1_b_q
                PieceInPosition.rook.id -> if (color == PieceInPosition.isWhite) R.drawable.t1_w_r else R.drawable.t1_b_r
                PieceInPosition.knight.id -> if (color == PieceInPosition.isWhite) R.drawable.t1_w_n else R.drawable.t1_b_n
                PieceInPosition.bishop.id -> if (color == PieceInPosition.isWhite) R.drawable.t1_w_b else R.drawable.t1_b_b
                else -> if (color == PieceInPosition.isWhite) R.drawable.t1_w_p else R.drawable.t1_b_p
            }
        }

        fun getTheme2(color: Int, piece: Int): Int {
            return when (piece) {
                PieceInPosition.king.id -> if (color == PieceInPosition.isWhite) R.drawable.t2_w_k else R.drawable.t2_b_k
                PieceInPosition.queen.id -> if (color == PieceInPosition.isWhite) R.drawable.t2_w_q else R.drawable.t2_b_q
                PieceInPosition.rook.id -> if (color == PieceInPosition.isWhite) R.drawable.t2_w_r else R.drawable.t2_b_r
                PieceInPosition.knight.id -> if (color == PieceInPosition.isWhite) R.drawable.t2_w_n else R.drawable.t2_b_n
                PieceInPosition.bishop.id -> if (color == PieceInPosition.isWhite) R.drawable.t2_w_b else R.drawable.t2_b_b
                else -> if (color == PieceInPosition.isWhite) R.drawable.t2_w_p else R.drawable.t2_b_p
            }
        }
    }
}