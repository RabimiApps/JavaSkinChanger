package com.rabimi.javaskinchanger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var skinImage: ImageView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnLibrary: Button

    private var selectedUri: Uri? = null
    private var mcToken: String? = null
    private val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        skinImage = findViewById(R.id.skinImage)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)

        // WelcomeActivity からの minecraft_token を受け取る
        mcToken = intent.getStringExtra("minecraft_token")
        val username = intent.getStringExtra("minecraft_username")

        val sp = getSharedPreferences("prefs", MODE_PRIVATE)

        if (mcToken != null) {
            // Token を保存しておく
            sp.edit().putString("minecraft_token", mcToken).apply()
            loadCurrentSkin(mcToken!!)
        } else {
            // 既存 token がある場合はそれを使用
            mcToken = sp.getString("minecraft_token", null)
            if (mcToken != null) {
                loadCurrentSkin(mcToken!!)
            } else {
                Toast.makeText(this, "ログインしてください", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, WelcomeActivity::class.java))
                finish()
                return
            }
        }

        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/png"
            startActivityForResult(intent, 1)
        }

        btnUpload.setOnClickListener {
            if (selectedUri != null && mcToken != null) {
                mainScope.launch {
                    val success = withContext(Dispatchers.IO) {
                        MinecraftSkinManager.uploadSkin(this@MainActivity, selectedUri!!, mcToken!!)
                    }
                    if (success) {
                        Toast.makeText(this@MainActivity, "スキンをアップロードしました", Toast.LENGTH_SHORT).show()
                        loadCurrentSkin(mcToken!!)
                        btnSelect.visibility = Button.VISIBLE
                        btnUpload.visibility = Button.GONE
                    } else {
                        Toast.makeText(this@MainActivity, "スキンアップロード失敗", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnLibrary.setOnClickListener {
            startActivity(Intent(this, SkinLibraryActivity::class.java))
        }
    }

    private fun loadCurrentSkin(token: String) {
        mainScope.launch {
            val skinUrl = withContext(Dispatchers.IO) {
                MinecraftSkinManager.getCurrentSkinUrl(token)
            }
            Glide.with(this@MainActivity).load(skinUrl).into(skinImage)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            selectedUri = data?.data
            selectedUri?.let {
                Glide.with(this).load(it).into(skinImage)
                btnSelect.visibility = Button.GONE
                btnUpload.visibility = Button.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        mainScope.cancel()
        super.onDestroy()
    }
}