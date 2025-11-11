package com.rabimi.javaskinchanger

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.github.storeforminecraft.skinview.SkinViewAndroid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var skinView: SkinViewAndroid
    private lateinit var skinImage: ImageView
    private lateinit var txtUsername: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnLibrary: Button
    private lateinit var btnLogout: Button

    private val mainScope = MainScope()
    private var selectedUri: Uri? = null
    private var mcToken: String? = null

    // ActivityResult for picking image
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            try {
                contentResolver.openInputStream(it).use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    if (bitmap != null) {
                        skinImage.setImageBitmap(bitmap)        // 2D プレビュー
                        skinView.setSkin(bitmap)               // SkinViewAndroid に適用
                    } else {
                        Toast.makeText(this, "画像の読み込みに失敗しました", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "画像を開けませんでした", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val skinView = findViewById<SkinView>(R.id.skinView)
        skinImage = findViewById(R.id.skinImage)
        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)
        btnLogout = findViewById(R.id.btnLogout)

        // SharedPreferences or intent からトークン/ユーザー名取得
        val sp = getSharedPreferences("prefs", MODE_PRIVATE)
        mcToken = intent.getStringExtra("minecraft_token") ?: sp.getString("minecraft_token", null)
        val username = intent.getStringExtra("minecraft_username") ?: sp.getString("minecraft_username", "不明")
        txtUsername.text = "ログイン中: $username"

        // オンラインスキンを取得して表示
        if (mcToken != null) {
            mainScope.launch {
                val url = withContext(Dispatchers.IO) {
                    try {
                        MinecraftSkinManager.getCurrentSkinUrl(mcToken!!)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                url?.let { loadSkinFromUrl(it) }
            }
        }

        btnSelect.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        btnUpload.setOnClickListener {
            val uri = selectedUri
            val token = mcToken
            if (uri == null) {
                Toast.makeText(this, "先に画像を選択してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (token == null) {
                Toast.makeText(this, "ログインしてください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mainScope.launch(Dispatchers.IO) {
                val success = try {
                    MinecraftSkinManager.uploadSkin(this@MainActivity, uri, token)
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(this@MainActivity, "スキンをアップロードしました", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "スキンアップロードに失敗しました", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnLibrary.setOnClickListener {
            Toast.makeText(this, "ライブラリを開きます (未実装)", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            sp.edit().clear().apply()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    private fun loadSkinFromUrl(url: String) {
        mainScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val stream = java.net.URL(url).openStream()
                    BitmapFactory.decodeStream(stream).also { stream.close() }
                }
                if (bitmap != null) {
                    skinImage.setImageBitmap(bitmap)
                    skinView.setSkin(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "スキンの読み込みに失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        // mainScope.cancel() のために kotlinx.coroutines.cancel をインポート済み
        mainScope.cancel()
        super.onDestroy()
    }
}