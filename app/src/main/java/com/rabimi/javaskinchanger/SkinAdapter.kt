package com.rabimi.javaskinchanger

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SkinAdapter(
    private val context: Context,
    private val skins: List<Uri>
) : RecyclerView.Adapter<SkinAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.skinImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_skin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uri = skins.getOrNull(position)
        if (uri != null) {
            Glide.with(context)
                .load(uri)
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.mipmap.ic_launcher) // デフォルト画像
        }
    }

    override fun getItemCount(): Int = skins.size
}