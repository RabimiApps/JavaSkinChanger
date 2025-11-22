package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream

class SkinLibraryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SELECTED_SKIN_PATH = "selected_skin_path"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SkinAdapter
    private lateinit var addButton: ImageView

    private val skins = mutableListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        recyclerView = findViewById(R.id.recyclerView)
        addButton = findViewById(R.id.btnAddSkin)

        recyclerView.layoutManager = GridLayoutManager(this, 4)
        adapter = SkinAdapter()
        recyclerView.adapter = adapter

        loadSkins()

        // ライブラリに追加
        addButton.setOnClickListener {
            val bmp = intent.getParcelableExtra<Bitmap>("currentSkin") ?: return@setOnClickListener
            saveSkinToLibrary(bmp)
        }
    }

    private fun loadSkins() {
        skins.clear()
        val dir = getSkinsDir()
        if (!dir.exists()) dir.mkdirs()
        dir.listFiles()?.let { skins.addAll(it) }
        adapter.notifyDataSetChanged()
    }

    private fun getSkinsDir(): File = File(filesDir, "skin_library")

    private fun saveSkinToLibrary(bitmap: Bitmap) {
        val dir = getSkinsDir()
        val filename = "skin_${System.currentTimeMillis()}.png"
        val file = File(dir, filename)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Toast.makeText(this, "スキン追加しました", Toast.LENGTH_SHORT).show()
            loadSkins()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "追加に失敗しました", Toast.LENGTH_SHORT).show()
        }
    }

    inner class SkinAdapter : RecyclerView.Adapter<SkinAdapter.SkinViewHolder>() {

        inner class SkinViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgSkin: ImageView = view.findViewById(R.id.imgSkinItem)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkinViewHolder {
            val view = layoutInflater.inflate(R.layout.item_skin, parent, false)
            return SkinViewHolder(view)
        }

        override fun onBindViewHolder(holder: SkinViewHolder, position: Int) {
            val file = skins[position]
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            holder.imgSkin.setImageBitmap(bmp)

            // 選択
            holder.imgSkin.setOnClickListener {
                val data = Intent().apply {
                    putExtra(EXTRA_SELECTED_SKIN_PATH, file.absolutePath)
                }
                setResult(Activity.RESULT_OK, data)
                finish()
            }

            // 削除
            holder.imgSkin.setOnLongClickListener {
                AlertDialog.Builder(this@SkinLibraryActivity)
                    .setTitle("削除確認")
                    .setMessage("このスキンを削除しますか？")
                    .setPositiveButton("削除") { _, _ ->
                        file.delete()
                        loadSkins()
                        Toast.makeText(this@SkinLibraryActivity, "削除しました", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("キャンセル", null)
                    .show()
                true
            }
        }

        override fun getItemCount(): Int = skins.size
    }
}