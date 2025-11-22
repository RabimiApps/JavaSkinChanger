package com.rabimi.javaskinchanger

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class SkinAdapter(
    private var paths: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SkinAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgSkin: ImageView = view.findViewById(R.id.imgSkinItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_skin, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = paths.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bmp = BitmapFactory.decodeFile(paths[position])
        holder.imgSkin.setImageBitmap(Bitmap.createScaledBitmap(bmp, 64, 64, true))
        holder.itemView.setOnClickListener { onClick(paths[position]) }
    }

    fun updateData(newPaths: List<String>) {
        paths = newPaths
        notifyDataSetChanged()
    }
}