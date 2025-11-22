package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream

class SkinLibraryActivity : AppCompatActivity() {

    companion object {
        const val RESULT_SKIN_BITMAP = "result_skin_bitmap"
        const val EXTRA_CURRENT_SKIN = "extra_current_skin"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SkinAdapter
    private lateinit var btnAddCurrent: ImageButton
    private val skinFiles = mutableListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        recyclerView = findViewById(R.id.recyclerViewSkins)
        recyclerView.layoutManager = GridLayoutManager(this, 4)

        btnAddCurrent = findViewById(R.id.btnAddCurrentSkin)

        loadLibrary()
        adapter = SkinAdapter(skinFiles.map { it.absolutePath }) { path ->
            val bmp = BitmapFactory.decodeFile(path)
            val resized = Bitmap.createScaledBitmap(bmp, 64, 64, true)
            val intent = Intent().apply {
                putExtra(RESULT_SKIN_BITMAP, bmpToByteArray(resized))
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
        recyclerView.adapter = adapter

        btnAddCurrent.setOnClickListener {
            val bytes = intent.getByteArrayExtra(EXTRA_CURRENT_SKIN) ?: return@setOnClickListener
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            saveSkinToLibrary(bmp)
        }
    }

    private fun loadLibrary() {
        val dir = getDir("skin_library", Context.MODE_PRIVATE)
        skinFiles.clear()
        skinFiles.addAll(dir.listFiles()?.toList() ?: emptyList())
    }

    private fun saveSkinToLibrary(bitmap: Bitmap) {
        val dir = getDir("skin_library", Context.MODE_PRIVATE)
        val file = File(dir, "skin_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        skinFiles.add(file)
        adapter.updateData(skinFiles.map { it.absolutePath })
    }

    private fun bmpToByteArray(bitmap: Bitmap): ByteArray {
        val baos = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        return baos.toByteArray()
    }
}