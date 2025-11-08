package com.rabimi.javaskinchanger

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class SkinLibraryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SkinAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skin_library)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 仮に取得したUriのリスト
        val uriList: List<Uri> = getSkinUris()

        // Uri -> File に変換
        val fileList: List<File> = uriList.mapNotNull { uri ->
            uri.path?.let { File(it) }
        }

        adapter = SkinAdapter(this, fileList)
        recyclerView.adapter = adapter
    }

    // サンプル: スキンのUriを取得する関数
    private fun getSkinUris(): List<Uri> {
        // 実際は端末ストレージやアプリ内ストレージから取得
        return listOf()
    }
}