package com.rabimi.javaskinchanger

import android.app.Activity
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
    private lateinit var btnSwitchModel: Button
    private lateinit var btnLibrary: Button

    private var selectedUri: Uri? = null
    private var isSteve = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ログイン済みならスキン画面へ
        if (MicrosoftAuth.isLoggedIn(this)) {
            setContentView(R.layout.activity_main)
        } else {
            // 初回はようこそ画面へ
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }

        skinImage = findViewById(R.id.skinImage)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnSwitchModel = findViewById(R.id.btnSwitchModel)
        btnLibrary = findViewById(R.id.btnLibrary)

        // 最初にログイン済みアカウントのスキンを表示
        val currentSkin = SkinManager.getCurrentSkin(this)
        Glide.with(this).load(currentSkin).into(skinImage)

        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/png"
            startActivityForResult(intent, 1)
        }

        btnUpload.setOnClickListener {
            selectedUri?.let {
                val name = System.currentTimeMillis().toString()
                val saved = SkinStorage.saveSkin(this, it, name)
                if (saved) Toast.makeText(this, "スキンを保存しました", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this, "スキン保存に失敗しました", Toast.LENGTH_SHORT).show()

                SkinManager.uploadSkin(this, it, isSteve)
            } ?: run {
                Toast.makeText(this, "スキンを選択してください", Toast.LENGTH_SHORT).show()
            }
        }

        btnSwitchModel.setOnClickListener {
            isSteve = !isSteve
            btnSwitchModel.text = if (isSteve) "スティーブ → アレックス" else "アレックス → スティーブ"
        }

        btnLibrary.setOnClickListener {
            startActivity(Intent(this, SkinLibraryActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            selectedUri = data?.data
            selectedUri?.let { Glide.with(this).load(it).into(skinImage) }
            btnSelect.visibility = Button.GONE
        }
    }
}