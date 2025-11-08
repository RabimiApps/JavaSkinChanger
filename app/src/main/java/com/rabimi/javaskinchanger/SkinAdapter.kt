package com.rabimi.javaskinchanger

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import com.bumptech.glide.Glide

class SkinAdapter(
    private val context: Context,
    private val skins: List<File>
) : RecyclerView.Adapter<SkinAdapter.ViewHolder>() { // ←ここを明示的に

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.skinImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_skin, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val skinFile = skins[position]
        Glide.with(context)
            .load(skinFile)
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = skins.size
}