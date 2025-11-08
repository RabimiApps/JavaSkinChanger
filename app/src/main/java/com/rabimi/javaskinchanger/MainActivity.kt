package com.rabimi.javaskinchanger

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
    private lateinit var btnLibrary: Button

    private var selectedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        skinImage = findViewById(R.id.skinImage)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)

        val sp = getSharedPreferences("prefs", MODE_PRIVATE)
        val token = sp.getString("microsoft_token", null)

        if (token != null) {
            loadCurrentSkin(token)
        } else {
            Toast.makeText(this, "ログインしてください", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/png"
            startActivityForResult(intent, 1)
        }

        btnUpload.setOnClickListener {
            if (selectedUri != null) {
                val spToken = sp.getString("microsoft_token", null)
                if (spToken != null) {
                    MinecraftSkinManager.uploadSkin(this, selectedUri!!, spToken)
                    Toast.makeText(this, "スキンをアップロードしました", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnLibrary.setOnClickListener {
            startActivity(Intent(this, SkinLibraryActivity::class.java))
        }
    }

    private fun loadCurrentSkin(token: String) {
        // 仮: Minecraft Skin URL を取得して表示する処理
        val skinUrl = MinecraftSkinManager.getCurrentSkinUrl(token)
        Glide.with(this).load(skinUrl).into(skinImage)
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
}