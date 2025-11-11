package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
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
    private var pendingBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ✅ SkinView3Dを動的に生成して配置
        val skinContainer = findViewById<FrameLayout>(R.id.skinContainer)
        skinView = SkinView3DSurfaceView(this)
        skinContainer.addView(skinView)

        skinImage = findViewById(R.id.skinImage)
        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)
        btnLogout = findViewById(R.id.btnLogout)

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val username = prefs.getString("minecraft_username", null)
        val token = prefs.getString("minecraft_token", null)

        // 🔸 ログインチェック
        if (username.isNullOrBlank() || token.isNullOrBlank()) {
            AlertDialog.Builder(this)
                .setTitle("ログインが必要です")
                .setMessage("ログイン情報が見つかりません。再ログインしてください。")
                .setPositiveButton("OK") { _, _ ->
                    startActivity(Intent(this, WelcomeActivity::class.java))
                    finish()
                }
                .setCancelable(false)
                .show()
            return
        }

        txtUsername.text = "ログイン中: $username"

        // 🔹 スキン選択
        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(intent, "スキンを選択"), REQUEST_SKIN_PICK)
        }

        // 🔹 アップロード（未実装）
        btnUpload.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("アップロード")
                .setMessage("アップロード処理はまだ実装されていません")
                .setPositiveButton("OK", null)
                .show()
        }

        // 🔹 スキンライブラリ
        btnLibrary.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("ライブラリ")
                .setMessage("スキンライブラリ機能はまだ実装されていません")
                .setPositiveButton("OK", null)
                .show()
        }

        // 🔹 ログアウト
        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

        // Surface準備
        skinView.holder.addCallback(object : android.view.SurfaceHolder.Callback {
            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                pendingBitmap?.let {
                    try {
                        skinView.render(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    pendingBitmap = null
                }
            }

            override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {}
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SKIN_PICK && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val bitmapOriginal: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    val bitmap = bitmapOriginal.copy(Bitmap.Config.ARGB_8888, true)
                    val resized = if (bitmap.width != 64 || bitmap.height != 64)
                        Bitmap.createScaledBitmap(bitmap, 64, 64, true)
                    else bitmap

                    skinImage.setImageBitmap(resized)

                    if (skinView.holder.surface.isValid) skinView.render(resized)
                    else pendingBitmap = resized

                } catch (e: Exception) {
                    e.printStackTrace()
                    AlertDialog.Builder(this)
                        .setTitle("エラー")
                        .setMessage("スキンの読み込みに失敗しました")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }
}