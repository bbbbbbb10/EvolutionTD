package com.bbb.evolutiontd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bbb.evolutiontd.model.FriendItem

class RequestsAdapter(
    val list: List<FriendItem>,
    val onAccept: (FriendItem) -> Unit,
    val onDecline: (FriendItem) -> Unit
) : RecyclerView.Adapter<RequestsAdapter.H>() {

    class H(v: View) : RecyclerView.ViewHolder(v) {
        val tvName: TextView = v.findViewById(R.id.tvRequestName)
        val btnAccept: Button = v.findViewById(R.id.btnAcceptFriend)
        val btnDecline: Button = v.findViewById(R.id.btnDeclineFriend)
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        H(LayoutInflater.from(p.context).inflate(R.layout.item_request, p, false))

    override fun onBindViewHolder(h: H, p: Int) {
        val item = list[p]
        h.tvName.text = "NEW: ${item.name}"
        h.btnAccept.setOnClickListener { onAccept(item) }
        h.btnDecline.setOnClickListener { onDecline(item) }
    }
    override fun getItemCount() = list.size
}