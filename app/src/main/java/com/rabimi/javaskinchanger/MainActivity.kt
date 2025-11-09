package com.rabimi.javaskinchanger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var skinImage: ImageView
    private lateinit var txtUsername: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnLibrary: Button
    private lateinit var btnLogout: Button

    private var selectedUri: Uri? = null
    private var mcToken: String? = null
    private val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        skinImage = findViewById(R.id.skinImage)
        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)
        btnLogout = findViewById(R.id.btnLogout)

        val sp = getSharedPreferences("prefs", MODE_PRIVATE)
        mcToken = intent.getStringExtra("minecraft_token") ?: sp.getString("minecraft_token", null)
        val username = intent.getStringExtra("minecraft_username") ?: sp.getString("minecraft_username", "不明")

        if (mcToken == null) {
            Toast.makeText(this, "ログインしてください", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        txtUsername.text = "ログイン中: $username"
        loadCurrentSkin(mcToken!!)

        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/png"
            startActivityForResult(intent, 1)
        }

        btnUpload.setOnClickListener {
            if (selectedUri != null) {
                mainScope.launch {
                    val success = withContext(Dispatchers.IO) {
                        MinecraftSkinManager.uploadSkin(this@MainActivity, selectedUri!!, mcToken!!)
                    }
                    if (success) {
                        Toast.makeText(this@MainActivity, "スキンをアップロードしました", Toast.LENGTH_SHORT).show()
                        loadCurrentSkin(mcToken!!)
                        fadeSwitch(btnUpload, btnSelect)
                    } else {
                        Toast.makeText(this@MainActivity, "スキンアップロード失敗", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnLibrary.setOnClickListener {
            startActivity(Intent(this, SkinLibraryActivity::class.java))
        }

        btnLogout.setOnClickListener {
            sp.edit().clear().apply()
            Toast.makeText(this, "ログアウトしました", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
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

    private fun fadeSwitch(hideBtn: Button, showBtn: Button) {
        hideBtn.animate().alpha(0f).setDuration(200).withEndAction {
            hideBtn.visibility = Button.GONE
            showBtn.alpha = 0f
            showBtn.visibility = Button.VISIBLE
            showBtn.animate().alpha(1f).setDuration(200).start()
        }.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            selectedUri = data?.data
            selectedUri?.let {
                Glide.with(this).load(it).into(skinImage)
                fadeSwitch(btnSelect, btnUpload)
            }
        }
    }

    override fun onDestroy() {
        mainScope.cancel()
        super.onDestroy()
    }
}