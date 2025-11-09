package com.rabimi.javaskinchanger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlinx.coroutines.*
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var sceneView: SceneView
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

        sceneView = findViewById(R.id.sceneView)
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

        // 現在のスキンをロードして3Dに適用
        mainScope.launch {
            val skinUrl = withContext(Dispatchers.IO) {
                MinecraftSkinManager.getCurrentSkinUrl(mcToken!!)
            }
            loadSkin3D(skinUrl)
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
                        // アップロード後も即座に3D表示更新
                        selectedUri?.let { loadSkin3D(it.toString()) }
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

    /**
     * 3DスキンをロードしてSceneViewに表示する
     */
    private fun loadSkin3D(skinUrl: String) {
        // 実際には MinecraftスキンPNGを3Dモデルに貼り付ける処理が必要
        // ここでは Sceneform の ModelRenderable を使う例
        ModelRenderable.builder()
            .setSource(this, Uri.parse("file:///android_asset/minecraft_character.sfb")) // デフォルトモデル
            .build()
            .thenAccept { renderable ->
                sceneView.scene.addChild(
                    com.google.ar.sceneform.Node().apply {
                        this.renderable = renderable
                        this.localScale = com.google.ar.sceneform.math.Vector3(1f, 1f, 1f)
                        this.setLookDirection(com.google.ar.sceneform.math.Vector3(0f, 0f, -1f))
                    }
                )
            }
            .exceptionally {
                Toast.makeText(this, "3Dモデルの読み込みに失敗しました", Toast.LENGTH_SHORT).show()
                null
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
                // 選んだスキンを即座に3D表示
                loadSkin3D(it.toString())
                fadeSwitch(btnSelect, btnUpload)
            }
        }
    }

    override fun onDestroy() {
        mainScope.cancel()
        sceneView.destroy()
        super.onDestroy()
    }
}