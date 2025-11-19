package com.rabimi.javaskinchanger

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import kotlinx.coroutines.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class MainActivity : AndroidApplication() {

    companion object { private const val TAG = "SkinDebug" }

    private lateinit var txtUsername: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnLibrary: Button
    private lateinit var btnLogout: Button
    private lateinit var switchModel: SwitchMaterial
    private lateinit var lblModel: TextView

    private lateinit var skinApp: Skin3DApp
    private val REQUEST_SKIN_PICK = 1001
    private var currentSkinBitmap: Bitmap? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val colorSelect = 0xFF4FC3F7.toInt()
    private val colorUploadTarget = 0xFF4CAF50.toInt()
    private val colorUploadInitial = 0xFFBDBDBD.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)
        btnLogout = findViewById(R.id.btnLogout)
        switchModel = findViewById(R.id.switchModel)
        lblModel = findViewById(R.id.lblModel)

        // --- libGDX 初期化 ---
        val container = findViewById<FrameLayout>(R.id.skinContainer)
        skinApp = Skin3DApp()
        val config = AndroidApplicationConfiguration()
        val gdxView = initializeForView(skinApp, config)
        container.addView(
            gdxView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setupUI()
        checkLogin()
        loadAccountSkinOrTest()
    }

    private fun setupUI() {
        btnSelect.backgroundTintList = ColorStateList.valueOf(colorSelect)
        btnSelect.text = "画像を選択"

        btnUpload.visibility = View.GONE
        btnUpload.backgroundTintList = ColorStateList.valueOf(colorUploadInitial)
        btnUpload.text = "アップロード"

        switchModel.setOnCheckedChangeListener { _, isChecked ->
            lblModel.text = if (isChecked) "モデル: Alex" else "モデル: Steve"
            currentSkinBitmap?.let { skinApp.updateSkin(it) }
        }

        btnSelect.setOnClickListener { selectSkinImage() }
        btnUpload.setOnClickListener { handleUpload() }
        btnLibrary.setOnClickListener {
            AlertDialog.Builder(this).setMessage("未実装").setPositiveButton("OK", null).show()
        }
        btnLogout.setOnClickListener {
            getSharedPreferences("prefs", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    private fun checkLogin() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val username = prefs.getString("minecraft_username", null)
        val token = prefs.getString("minecraft_token", null)
        if (username == null || token == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        } else {
            txtUsername.text = "ログイン中: $username"
        }
    }

    private fun loadAccountSkinOrTest() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val token = prefs.getString("minecraft_token", null)

        if (token == null) {
            val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(0xFFFF0000.toInt())
            currentSkinBitmap = bmp
            skinApp.updateSkin(bmp)
            return
        }

        scope.launch {
            try {
                val conn = (URL("https://api.minecraftservices.com/minecraft/profile").openConnection() as HttpURLConnection)
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                if (conn.responseCode == 200) {
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    conn.disconnect()

                    val skins = json.optJSONArray("skins")
                    if (skins != null && skins.length() > 0) {
                        val skinUrl = skins.getJSONObject(0).getString("url").replace("http://", "https://")
                        val stream: InputStream = URL(skinUrl).openStream()
                        val bmp = BitmapFactory.decodeStream(stream)
                        val fixed = Bitmap.createScaledBitmap(bmp, 64, 64, true)

                        currentSkinBitmap = fixed
                        withContext(Dispatchers.Main) {
                            skinApp.updateSkin(fixed)
                        }
                        return@launch
                    }
                }

            } catch (e: Exception) {
                Log.w(TAG, "loadAccountSkinOrTest failed: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(0xFFFF0000.toInt())
                currentSkinBitmap = bmp
                skinApp.updateSkin(bmp)
            }
        }
    }

    private fun selectSkinImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(Intent.createChooser(intent, "スキンを選択"), REQUEST_SKIN_PICK)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == REQUEST_SKIN_PICK && res == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val orig = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                // 強制的に64x64にリサイズ
                val bmp = Bitmap.createScaledBitmap(orig.copy(Bitmap.Config.ARGB_8888, true), 64, 64, true)
                currentSkinBitmap = bmp
                skinApp.updateSkin(bmp)

                btnUpload.visibility = View.VISIBLE
                btnUpload.backgroundTintList = ColorStateList.valueOf(colorUploadTarget)
            } catch (e: Exception) {
                AlertDialog.Builder(this)
                    .setTitle("エラー")
                    .setMessage("スキンの読み込みに失敗しました: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun handleUpload() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val token = prefs.getString("minecraft_token", null) ?: return

        val bitmap = currentSkinBitmap ?: return
        scope.launch {
            try {
                // bitmap → PNG byte[]
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val imageBytes = baos.toByteArray()

                val modelType = if (switchModel.isChecked) "slim" else "classic"

                val boundary = "*****" + System.currentTimeMillis()
                val lineEnd = "\r\n"
                val twoHyphens = "--"

                val url = URL("https://api.minecraftservices.com/minecraft/profile/skins")
                val conn = url.openConnection() as HttpURLConnection
                conn.doInput = true
                conn.doOutput = true
                conn.useCaches = false
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")

                val outputStream = DataOutputStream(conn.outputStream)
                outputStream.writeBytes(twoHyphens + boundary + lineEnd)
                outputStream.writeBytes("Content-Disposition: form-data; name=\"model\"$lineEnd$lineEnd")
                outputStream.writeBytes(modelType + lineEnd)
                outputStream.writeBytes(twoHyphens + boundary + lineEnd)
                outputStream.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"$lineEnd")
                outputStream.writeBytes("Content-Type: image/png$lineEnd$lineEnd")
                outputStream.write(imageBytes)
                outputStream.writeBytes(lineEnd)
                outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
                outputStream.flush()
                outputStream.close()

                val responseCode = conn.responseCode
                val responseMessage = conn.inputStream.bufferedReader().readText()
                conn.disconnect()

                withContext(Dispatchers.Main) {
                    if (responseCode in 200..299) {
                        btnUpload.backgroundTintList = ColorStateList.valueOf(0xFF8BC34A.toInt())
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("成功")
                            .setMessage("スキンがアップロードされました")
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("失敗")
                            .setMessage("アップロード失敗: $responseCode\n$responseMessage")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("エラー")
                        .setMessage("アップロード中にエラー: ${e.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
