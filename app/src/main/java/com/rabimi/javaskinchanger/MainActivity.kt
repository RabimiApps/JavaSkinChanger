package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.steveice10.skinview.SkinView

class MainActivity : AppCompatActivity() {

    private lateinit var skinView: SkinView
    private lateinit var skinImage: ImageView
    private lateinit var txtUsername: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnLibrary: Button
    private lateinit var btnLogout: Button

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val inputStream = contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            skinImage.setImageBitmap(bitmap)
            skinView.setSkin(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        skinView = findViewById(R.id.skinView)
        skinImage = findViewById(R.id.skinImage)
        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)
        btnLogout = findViewById(R.id.btnLogout)

        btnSelect.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        // 必要に応じてボタンの処理追加
        btnUpload.setOnClickListener { /* アップロード処理 */ }
        btnLibrary.setOnClickListener { /* ライブラリ表示処理 */ }
        btnLogout.setOnClickListener { finish() }
    }
}