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
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    companion object { private const val TAG = "SkinDebug" }

    private lateinit var txtUsername: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnLibrary: Button
    private lateinit var btnLogout: Button
    private lateinit var switchModel: SwitchMaterial
    private lateinit var lblModel: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var skinView: ImageView

    private val REQUEST_SKIN_PICK = 1001
    private var currentSkinBitmap: Bitmap? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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
        skinView = findViewById(R.id.imgSkin)

        setupUI()
        checkLogin()

        // ☆ 初期スキンを steve.png に強制
        loadDefaultSteveSkin()
    }

    /** res/raw/steve.png のビットマップを読み込んで表示する */
    private fun loadDefaultSteveSkin() {
        val input = resources.openRawResource(R.raw.steve)
        val bmp = BitmapFactory.decodeStream(input)
        val resized = resizeTo64(bmp)

        currentSkinBitmap = resized
        skinView.setImageBitmap(resized)
    }

    private fun setupUI() {
        btnSelect.backgroundTintList = ColorStateList.valueOf(colorSelect)
        btnUpload.backgroundTintList = ColorStateList.valueOf(colorUploadInitial)
        btnUpload.visibility = View.GONE
        progressBar.visibility = View.GONE

        switchModel.setOnCheckedChangeListener { _, isChecked ->
            lblModel.text = if (isChecked) "モデル: Alex" else "モデル: Steve"
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

    /** ファイル選択 */
    private fun selectSkinImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(Intent.createChooser(intent, "スキンを選択"), REQUEST_SKIN_PICK)
    }

    /** 選択後即プレビュー */
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == REQUEST_SKIN_PICK && res == Activity.RESULT_OK) {
            val uri = data?.data ?: return

            try {
                val orig = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val bmp = resizeTo64(orig)

                currentSkinBitmap = bmp
                skinView.setImageBitmap(bmp)

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

    private fun resizeTo64(bitmap: Bitmap): Bitmap {
        return Bitmap.createScaledBitmap(
            bitmap.copy(Bitmap.Config.ARGB_8888, true),
            64,
            64,
            true
        )
    }

    /** アップロード */
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

        scope.launch {
            val ok = uploadSkin(mcToken, bmp, modelType) { progress ->
                progressBar.progress = progress
            }

            progressBar.visibility = View.GONE
            Toast.makeText(
                this@MainActivity,
                if (ok) "アップロード完了" else "アップロード失敗",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /** アップロード本体 */
    private suspend fun uploadSkin(
        token: String,
        bmp: Bitmap,
        model: String,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
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

            val out = DataOutputStream(conn.outputStream)

            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"variant\"\r\n\r\n")
            out.writeBytes("$model\r\n")

            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\n")
            out.writeBytes("Content-Type: image/png\r\n\r\n")

            val pngBaos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, pngBaos)
            val bytes = pngBaos.toByteArray()

            val chunk = if (bytes.size >= 100) bytes.size / 100 else bytes.size
            if (chunk <= 0) {
                out.write(bytes)
                onProgress(100)
            } else {
                for (i in 0 until 100) {
                    val start = i * chunk
                    val end = if (i == 99) bytes.size else (i + 1) * chunk
                    if (start >= bytes.size) break
                    out.write(bytes, start, end - start)
                    onProgress(i + 1)
                }
            }

            out.writeBytes("\r\n--$boundary--\r\n")
            out.flush()
            out.close()

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