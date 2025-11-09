package com.rabimi.javaskinchanger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.rajawali3d.Object3D
import org.rajawali3d.materials.Material
import org.rajawali3d.materials.textures.Texture
import org.rajawali3d.renderer.Renderer
import org.rajawali3d.view.SurfaceView
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var rajawaliView: SurfaceView
    private lateinit var txtUsername: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnLibrary: Button
    private lateinit var btnLogout: Button

    private var selectedUri: Uri? = null
    private var mcToken: String? = null
    private val mainScope = MainScope()
    private lateinit var skinRenderer: SkinRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rajawaliView = findViewById(R.id.rajawaliView)
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

        // Rajawali Renderer 初期化
        skinRenderer = SkinRenderer(this)
        rajawaliView.setSurfaceRenderer(skinRenderer)

        // 現在のスキンをロード
        mainScope.launch {
            val skinUrl = withContext(Dispatchers.IO) {
                MinecraftSkinManager.getCurrentSkinUrl(mcToken!!)
            }
            skinRenderer.loadSkin(skinUrl)
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
                        selectedUri?.let { skinRenderer.loadSkin(it.toString()) }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            selectedUri = data?.data
            selectedUri?.let {
                skinRenderer.loadSkin(it.toString())
                fadeSwitch(btnSelect, btnUpload)
            }
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

    override fun onDestroy() {
        mainScope.cancel()
        super.onDestroy()
    }
}