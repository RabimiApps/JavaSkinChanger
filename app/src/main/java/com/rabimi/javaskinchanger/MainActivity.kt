package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {

    private lateinit var skinImage: ImageView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnSwitchModel: Button
    private lateinit var btnLibrary: Button

    private var selectedUri: Uri? = null
    private var isSteve = true  // true = スティーブ, false = アレックス

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ビューの取得
        skinImage = findViewById(R.id.skinImage)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnSwitchModel = findViewById(R.id.btnSwitchModel)
        btnLibrary = findViewById(R.id.btnLibrary)

        // スキン選択
        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/png"
            startActivityForResult(intent, 1)
        }

        // アップロード
        btnUpload.setOnClickListener {
            selectedUri?.let {
                val name = System.currentTimeMillis().toString()
                // ローカル保存
                val saved = SkinStorage.saveSkin(this, it, name)
                if (saved) {
                    Toast.makeText(this, "スキンを保存しました", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "スキン保存に失敗しました", Toast.LENGTH_SHORT).show()
                }
                // アップロード処理（仮）
                SkinManager.uploadSkin(this, it, isSteve)
            } ?: run {
                Toast.makeText(this, "スキンを選択してください", Toast.LENGTH_SHORT).show()
            }
        }

        // スティーブ/アレックス切替
        btnSwitchModel.setOnClickListener {
            isSteve = !isSteve
            btnSwitchModel.text = if (isSteve) "スティーブ → アレックス" else "アレックス → スティーブ"
        }

        // ライブラリ表示
        btnLibrary.setOnClickListener {
            val intent = Intent(this, SkinLibraryActivity::class.java)
            startActivity(intent)
        }
    }

    // スキン選択結果
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedUri = uri
                // 画像を ImageView に表示
                Glide.with(this).load(uri).into(skinImage)
            }
        }
    }
}