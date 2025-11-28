package com.bbb.evolutiontd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LeaderboardAdapter(private val list: List<UserRecord>) : RecyclerView.Adapter<LeaderboardAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val tvRank: TextView = v.findViewById(R.id.tvRank)
        val tvName: TextView = v.findViewById(R.id.tvName)
        val tvWave: TextView = v.findViewById(R.id.tvWave)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_leaderboard, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = list[position]
        holder.tvRank.text = "#${position + 1}"
        holder.tvName.text = item.name
        holder.tvWave.text = "Wave ${item.bestWave}"

        // Подсветка топ 3
        val color = when(position) {
            0 -> 0xFFFFD700.toInt() // Gold
            1 -> 0xFFC0C0C0.toInt() // Silver
            2 -> 0xFFCD7F32.toInt() // Bronze
            else -> 0xFFFFFFFF.toInt() // White
        }
        holder.tvRank.setTextColor(color)
    }

    override fun getItemCount(): Int = list.size
}