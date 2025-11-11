package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.storeforminecraft.skinviewandroid.library.threedimension.ui.SkinView3DSurfaceView
import kotlinx.coroutines.*
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var txtUsername: TextView
    private lateinit var btnUpload: Button
    private lateinit var btnSelect: Button
    private lateinit var btnLogout: Button
    private lateinit var btnLibrary: Button
    private lateinit var skinView: SkinView3DSurfaceView
    private lateinit var skinImage: ImageView

    private var skinBitmap: Bitmap? = null
    private val PICK_IMAGE = 100

    private val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtUsername = findViewById(R.id.txtUsername)
        btnUpload = findViewById(R.id.btnUpload)
        btnSelect = findViewById(R.id.btnSelect)
        btnLogout = findViewById(R.id.btnLogout)
        btnLibrary = findViewById(R.id.btnLibrary)
        skinView = findViewById(R.id.skinView)
        skinImage = findViewById(R.id.skinImage)

        // 🔹 SharedPreferences からトークンとユーザー名を取得
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val mcToken = prefs.getString("minecraft_token", null)
        val username = prefs.getString("minecraft_username", "ゲスト")
        txtUsername.text = "ログイン中: $username"

        // 🔹 画像選択
        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }

        // 🔹 スキンアップロード
        btnUpload.setOnClickListener {
            if (skinBitmap == null) {
                Toast.makeText(this, "スキン画像を選択してください", Toast.LENGTH_SHORT).show()
            } else if (mcToken != null) {
                mainScope.launch {
                    val success = uploadSkin(skinBitmap!!, mcToken)
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            if (success) "アップロード成功" else "アップロード失敗",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, "ログインしてください", Toast.LENGTH_SHORT).show()
            }
        }

        // 🔹 ログアウト
        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

        // 🔹 ライブラリ
        btnLibrary.setOnClickListener {
            Toast.makeText(this, "スキンライブラリ機能は未実装", Toast.LENGTH_SHORT).show()
        }

        // 🔹 デフォルトのスキンを取得して表示
        if (mcToken != null) {
            mainScope.launch {
                val bitmap = fetchSkin(mcToken)
                if (bitmap != null) {
                    skinBitmap = bitmap
                    runOnUiThread {
                        skinImage.setImageBitmap(bitmap)
                        skinView.setSkinBitmap(bitmap) // 3D表示
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                skinBitmap = BitmapFactory.decodeStream(inputStream)
                skinImage.setImageBitmap(skinBitmap)
                skinBitmap?.let { skinView.setSkinBitmap(it) }
            }
        }
    }

    // 🔹 Minecraft API からスキン取得（PNG）
    private suspend fun fetchSkin(token: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://api.minecraftservices.com/minecraft/profile/skins").openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            if (conn.responseCode in 200..299) {
                val skinJson = conn.inputStream.bufferedReader().readText()
                val url = JSONObject(skinJson)
                    .getJSONArray("skins")
                    .getJSONObject(0)
                    .getString("url")
                return@withContext BitmapFactory.decodeStream(URL(url).openStream())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    // 🔹 Minecraft API にスキンアップロード
    private suspend fun uploadSkin(bitmap: Bitmap, token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 簡易的な例: PNGをByteArrayに変換してPUTリクエスト
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val bytes = stream.toByteArray()

            val url = URL("https://api.minecraftservices.com/minecraft/profile/skins")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST" // POST か PUT
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "image/png")
            conn.doOutput = true
            conn.outputStream.use { it.write(bytes) }

            conn.responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun onDestroy() {
        mainScope.cancel()
        super.onDestroy()
    }
}