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
import android.view.SurfaceHolder
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
    private var pendingBitmap: Bitmap? = null // Surface準備まで保留するBitmap

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

        // Surface準備ができたら保留中Bitmapを反映
        skinView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                pendingBitmap?.let {
                    skinView.render(it)
                    pendingBitmap = null
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })

        // 画像選択ボタン
        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(intent, "スキンを選択"), REQUEST_SKIN_PICK)
        }

        // アップロードボタン（仮）
        btnUpload.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("アップロード")
                .setMessage("アップロード処理はまだ実装されていません")
                .setPositiveButton("OK", null)
                .show()
        }

        // スキンライブラリ（仮）
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
                // BitmapをARGB_8888に変換して安全に渡す
                val bitmapOriginal: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val bitmap = bitmapOriginal.copy(Bitmap.Config.ARGB_8888, true)

                // 2Dプレビューを即更新
                skinImage.setImageBitmap(bitmap)

                // 3Dビューに反映（Surface準備できてなければpendingBitmapに保留）
                if (skinView.holder.surface.isValid) {
                    skinView.render(bitmap)
                } else {
                    pendingBitmap = bitmap
                }
            }
        }
    }
}