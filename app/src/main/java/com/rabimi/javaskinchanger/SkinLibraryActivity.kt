package com.rabimi.javaskinchanger

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SkinLibraryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SkinAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skin_library)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3) // 3列グリッド

        val uriList: List<Uri> = getSkinUris() // 仮のスキンUriリスト
        adapter = SkinAdapter(this, uriList)
        recyclerView.adapter = adapter
    }

    private fun getSkinUris(): List<Uri> {
        // 実際は端末ストレージやアプリ内ストレージから Uri を取得
        // テスト用に空リストで問題なし
        return listOf()
    }
}