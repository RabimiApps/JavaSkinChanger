package com.rabimi.javaskinchanger

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.bumptech.glide.Glide
import java.io.File

class SkinAdapter(private val context: Context, private val skins: List<File>) : BaseAdapter() {

    override fun getCount(): Int = skins.size
    override fun getItem(position: Int): Any = skins[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val imageView = convertView as? ImageView ?: ImageView(context)
        imageView.layoutParams = ViewGroup.LayoutParams(200, 300)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(context).load(skins[position]).into(imageView)
        return imageView
    }
}