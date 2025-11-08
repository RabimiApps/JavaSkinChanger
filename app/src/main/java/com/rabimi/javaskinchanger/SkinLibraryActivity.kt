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
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        val uriList: List<Uri> = getSkinUris()

        // 空リストでも Adapter は作る
        adapter = SkinAdapter(this, uriList)
        recyclerView.adapter = adapter
    }

    private fun getSkinUris(): List<Uri> {
        // ここで端末内の画像Uriを取得する
        // 今は空リストだが、将来的にはFileProviderやMediaStoreから取得
        return listOf()
    }
}