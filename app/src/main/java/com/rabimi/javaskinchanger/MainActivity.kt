package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import dev.storeforminecraft.skinviewandroid.library.threedimension.ui.SkinView3DSurfaceView

class MainActivity : AppCompatActivity() {

    private lateinit var skinView: SkinView3DSurfaceView
    private lateinit var skinImage: ImageView
    private lateinit var txtUsername: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnLibrary: Button
    private lateinit var btnLogout: Button

    private val REQUEST_SKIN_PICK = 1001

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

        // ログイン済みユーザー名を取得
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val username = prefs.getString("minecraft_username", "ログイン中: ...")
        txtUsername.text = "ログイン中: $username"

        // 画像選択ボタン
        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }
            startActivityForResult(Intent.createChooser(intent, "スキンを選択"), REQUEST_SKIN_PICK)
        }

        // アップロードボタン（実際のAPI連携は別途）
        btnUpload.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("アップロード")
                .setMessage("アップロード処理はまだ実装されていません")
                .setPositiveButton("OK", null)
                .show()
        }

        // スキンライブラリ
        btnLibrary.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("ライブラリ")
                .setMessage("スキンライブラリ機能はまだ実装されていません")
                .setPositiveButton("OK", null)
                .show()
        }

        // ログアウト
        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SKIN_PICK && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val bitmap: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

                // 3Dスキンに即反映
                skinView.setSkin(bitmap)

                // 2Dプレビューも更新
                skinImage.setImageBitmap(bitmap)
            }
        }
    }
}