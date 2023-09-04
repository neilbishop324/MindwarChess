package com.neilb.mindwarchess.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.neilb.mindwarchess.databinding.KillPieceRowBinding
import com.neilb.mindwarchess.game.PieceThemes

class KillPieceAdapter : RecyclerView.Adapter<KillPieceAdapter.KillPieceHolder>() {

    var pieces: List<Pair<Int, Int>> = listOf()

    class KillPieceHolder(val binding: KillPieceRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KillPieceHolder {
        val binding = KillPieceRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KillPieceHolder(binding)
    }

    override fun onBindViewHolder(holder: KillPieceHolder, position: Int) {
        val piece = pieces[position]
        val res = PieceThemes.getTheme2(piece.first, piece.second)
        holder.binding.killPieceImage.setImageResource(res)
    }

    override fun getItemCount(): Int = pieces.size
}