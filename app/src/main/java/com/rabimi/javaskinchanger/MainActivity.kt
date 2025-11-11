package com.rabimi.javaskinchanger

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import dev.storeforminecraft.skinviewandroid.library.threedimension.ui.SkinView3DSurfaceView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var skinView: SkinView3DSurfaceView
    private lateinit var skinImage: ImageView
    private lateinit var txtUsername: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnLibrary: Button
    private lateinit var btnLogout: Button

    private val mainScope = MainScope()
    private var selectedUri: Uri? = null
    private var mcToken: String? = null

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            try {
                contentResolver.openInputStream(it).use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    if (bitmap != null) {
                        skinImage.setImageBitmap(bitmap)
                        setSkinToSkinView(bitmap)
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

        skinView = findViewById(R.id.skinView)
        skinImage = findViewById(R.id.skinImage)
        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)
        btnLogout = findViewById(R.id.btnLogout)

        val sp = getSharedPreferences("prefs", MODE_PRIVATE)
        mcToken = intent.getStringExtra("minecraft_token") ?: sp.getString("minecraft_token", null)
        val username = intent.getStringExtra("minecraft_username") ?: sp.getString("minecraft_username", "不明")
        txtUsername.text = "ログイン中: $username"

        // mcToken のチェックを厳密に
        if (mcToken.isNullOrEmpty()) {
            Toast.makeText(this, "ログイントークンが無効です", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        // 既存スキンを読み込む
        mainScope.launch {
            val url = try {
                withContext(Dispatchers.IO) {
                    MinecraftSkinManager.getCurrentSkinUrl(mcToken!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            url?.let { loadSkinFromUrl(it) }
        }

        btnSelect.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        btnUpload.setOnClickListener {
            val uri = selectedUri
            if (uri == null) {
                Toast.makeText(this, "先に画像を選択してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mainScope.launch(Dispatchers.IO) {
                val success = try {
                    MinecraftSkinManager.uploadSkin(this@MainActivity, uri, mcToken!!)
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
                bitmap?.let {
                    skinImage.setImageBitmap(it)
                    setSkinToSkinView(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "スキンの読み込みに失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setSkinToSkinView(bitmap: Bitmap) {
        try {
            skinView.render(bitmap)  // 3D SkinView に描画
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "3D Skin表示に失敗しました", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        mainScope.cancel()
        super.onDestroy()
    }
}