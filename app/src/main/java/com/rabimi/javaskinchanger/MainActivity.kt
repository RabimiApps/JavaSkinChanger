package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AndroidApplication() {

    companion object { private const val TAG = "SkinDebug" }

    private lateinit var txtUsername: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnLibrary: Button
    private lateinit var btnLogout: Button
    private lateinit var switchModel: SwitchMaterial
    private lateinit var lblModel: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var skinApp: Skin3DApp
    private val REQUEST_SKIN_PICK = 1001
    private var currentSkinBitmap: Bitmap? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main) // UI を使うことが多いので Main をベースにする

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
        progressBar = findViewById(R.id.progressBar)

        val container = findViewById<FrameLayout>(R.id.skinContainer)
        skinApp = Skin3DApp()
        val config = AndroidApplicationConfiguration()
        val gdxView = initializeForView(skinApp, config)
        container.addView(gdxView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        setupUI()
        checkLogin()
        loadAccountSkinOrTest()
    }

    private fun setupUI() {
        btnSelect.backgroundTintList = ColorStateList.valueOf(colorSelect)
        btnUpload.backgroundTintList = ColorStateList.valueOf(colorUploadInitial)
        btnUpload.visibility = View.GONE
        progressBar.visibility = View.GONE

        switchModel.setOnCheckedChangeListener { _, isChecked ->
            lblModel.text = if (isChecked) "モデル: Alex" else "モデル: Steve"
            skinApp.setModelType(if (isChecked) "slim" else "classic")
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
        // すでに選ばれているスキンがあればそれを優先
        if (currentSkinBitmap != null) {
            skinApp.updateSkin(currentSkinBitmap!!)
            return
        }

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val mcToken = prefs.getString("minecraft_token", null) ?: return

        // Coroutine 内でネットワーク IO を行う（fetchMinecraftSkin は suspend）
        scope.launch {
            val skinBitmap = fetchMinecraftSkin(mcToken) // suspend safe
            val bmp = skinBitmap ?: Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFFFF0000.toInt()) }
            currentSkinBitmap = bmp
            skinApp.updateSkin(bmp)
        }
    }

    // suspend にして IO スレッドで安全に動くようにする
    private suspend fun fetchMinecraftSkin(token: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.minecraftservices.com/minecraft/profile")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val resp = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(resp)

            // JSON の構造が変わる可能性があるので安全に取り出す
            val skins = json.optJSONArray("skins")
            if (skins == null || skins.length() == 0) return@withContext null
            val skinUrl = skins.getJSONObject(0).optString("url", null) ?: return@withContext null

            val skinConn = URL(skinUrl).openConnection() as HttpURLConnection
            skinConn.connectTimeout = 10000
            skinConn.readTimeout = 10000
            val bmp = BitmapFactory.decodeStream(skinConn.inputStream) ?: return@withContext null

            Bitmap.createScaledBitmap(bmp, 64, 64, true)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun selectSkinImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(Intent.createChooser(intent, "スキンを選択"), REQUEST_SKIN_PICK)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == REQUEST_SKIN_PICK && res == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            try {
                val orig = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val bmp = Bitmap.createScaledBitmap(orig.copy(Bitmap.Config.ARGB_8888, true), 64, 64, true)
                currentSkinBitmap = bmp
                skinApp.updateSkin(bmp)

                btnUpload.visibility = View.VISIBLE
                btnUpload.backgroundTintList = ColorStateList.valueOf(colorUploadTarget)
            } catch (e: Exception) {
                e.printStackTrace()
                AlertDialog.Builder(this)
                    .setTitle("エラー")
                    .setMessage("スキンの読み込みに失敗しました: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun handleUpload() {
        val bmp = currentSkinBitmap ?: run {
            Toast.makeText(this, "スキンが選択されていません", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val mcToken = prefs.getString("minecraft_token", null) ?: return
        val modelType = if (switchModel.isChecked) "slim" else "classic"

        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0

        // uploadSkin は suspend なので coroutine 内で呼ぶ
        scope.launch {
            val success = uploadSkin(mcToken, bmp, modelType) { progress ->
                // プログレスは UI スレッドで更新
                withContext(Dispatchers.Main) { progressBar.progress = progress }
            }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, if (success) "アップロード完了" else "アップロード失敗", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // suspend 化して IO で実行するようにした（進捗コールバックは保持）
    private suspend fun uploadSkin(token: String, bmp: Bitmap, model: String, onProgress: (Int) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val boundary = "----RabimiSkinBoundary"
                val url = URL("https://api.minecraftservices.com/minecraft/profile/skins")
                val conn = url.openConnection() as HttpURLConnection
                conn.doOutput = true
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val baos = ByteArrayOutputStream()
                baos.write("--$boundary\r\n".toByteArray())
                baos.write("Content-Disposition: form-data; name=\"model\"\r\n\r\n$model\r\n".toByteArray())
                baos.write("--$boundary\r\n".toByteArray())
                baos.write("Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n".toByteArray())
                baos.write("Content-Type: image/png\r\n\r\n".toByteArray())

                val skinBytes = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 100, skinBytes)
                val byteArray = skinBytes.toByteArray()
                val chunkSize = if (byteArray.size >= 100) byteArray.size / 100 else byteArray.size

                if (chunkSize <= 0) {
                    baos.write(byteArray)
                    onProgress(100)
                } else {
                    for (i in 0 until 100) {
                        val start = i * chunkSize
                        val end = if (i == 99) byteArray.size else (i + 1) * chunkSize
                        if (start >= byteArray.size) break
                        baos.write(byteArray, start, end - start)
                        onProgress(i + 1)
                    }
                }

                baos.write("\r\n--$boundary--\r\n".toByteArray())
                val out = DataOutputStream(conn.outputStream)
                out.write(baos.toByteArray())
                out.flush()
                out.close()

                // 応答を読みつつ終了コードを確認
                conn.inputStream.use { it.readBytes() }
                val rc = conn.responseCode
                rc in 200..299
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
