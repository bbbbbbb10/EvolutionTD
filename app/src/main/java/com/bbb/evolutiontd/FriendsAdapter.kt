package com.bbb.evolutiontd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bbb.evolutiontd.model.FriendItem

class FriendsAdapter(
    val list: List<FriendItem>,
    val onChat: (FriendItem) -> Unit,
    val onDelete: (FriendItem) -> Unit
) : RecyclerView.Adapter<FriendsAdapter.H>() {

    class H(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvFriendName)
        val btnChat: Button = v.findViewById(R.id.btnPrivateChat)
        val btnDelete: Button = v.findViewById(R.id.btnDeleteFriend)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        H(LayoutInflater.from(p.context).inflate(R.layout.item_friend, p, false))

    // Внутри FriendsAdapter в методе onBindViewHolder:

    override fun onBindViewHolder(h: H, p: Int) {
        val item = list[p]
        h.tvName.text = "👤 ${item.name}"

        // Сделаем View круглым (программная отрисовка круга)
        val statusDot = h.itemView.findViewById<View>(R.id.viewOnlineStatus)
        val shape = android.graphics.drawable.GradientDrawable()
        shape.shape = android.graphics.drawable.GradientDrawable.OVAL

        // Слушаем статус друга в реальном времени
        FirebaseHelper.getFriendStatus(item.uid) { isOnline ->
            if (isOnline) {
                shape.setColor(android.graphics.Color.GREEN) // Зеленый если в сети
            } else {
                shape.setColor(android.graphics.Color.RED)   // Красный если оффлайн
            }
            statusDot.background = shape
        }

        h.btnChat.setOnClickListener { onChat(item) }
        h.btnDelete.setOnClickListener { onDelete(item) }
    }
    override fun getItemCount() = list.size
}


