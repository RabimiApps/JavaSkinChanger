package com.rabimi.javaskinchanger

import android.os.Bundle
import android.widget.GridView
import androidx.appcompat.app.AppCompatActivity

class SkinLibraryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skin_library)

        val grid = findViewById<GridView>(R.id.skinGrid)
        val skins = SkinStorage.listSkins(this)
        grid.adapter = SkinAdapter(this, skins)
    }
}