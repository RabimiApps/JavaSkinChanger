package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {

    private lateinit var skinImage: ImageView
    private lateinit var btnSelect: MaterialButton
    private lateinit var btnUpload: MaterialButton
    private lateinit var btnSwitchModel: MaterialButton
    private lateinit var btnLibrary: MaterialButton

    private var selectedUri: Uri? = null
    private var isSteve = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        skinImage = findViewById(R.id.skinImage)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnSwitchModel = findViewById(R.id.btnSwitchModel)
        btnLibrary = findViewById(R.id.btnLibrary)

        // 初期状態
        btnUpload.isEnabled = false

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
                val saved = SkinStorage.saveSkin(this, it, name)
                if (saved) Toast.makeText(this, "スキンを保存しました", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this, "スキン保存に失敗しました", Toast.LENGTH_SHORT).show()
                SkinManager.uploadSkin(this, it, isSteve)
                btnUpload.isEnabled = false
            }
        }

        // モデル切替
        btnSwitchModel.setOnClickListener {
            isSteve = !isSteve
            btnSwitchModel.text = if (isSteve) "スティーブ → アレックス" else "アレックス → スティーブ"
        }

        // スキンライブラリー
        btnLibrary.setOnClickListener {
            startActivity(Intent(this, SkinLibraryActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            selectedUri = data?.data
            selectedUri?.let {
                Glide.with(this).load(it).into(skinImage)
                btnSelect.visibility = ImageView.GONE
                btnUpload.isEnabled = true
            }
        }
    }
}