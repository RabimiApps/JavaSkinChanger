package com.rabimi.javaskinchanger

import android.app.Activity
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
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import dev.storeforminecraft.skinviewandroid.library.threedimension.ui.SkinView3DSurfaceView
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    companion object { private const val TAG = "SkinDebug" }

    private lateinit var skinView: SkinView3DSurfaceView
    private lateinit var txtUsername: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnLibrary: Button
    private lateinit var btnLogout: Button
    private lateinit var switchModel: SwitchMaterial
    private lateinit var lblModel: TextView

    private val REQUEST_SKIN_PICK = 1001

    private var currentSkinBitmap: Bitmap? = null
    private var hasSelectedSkin = false

    private val colorSelect = 0xFF4FC3F7.toInt()
    private val colorUploadTarget = 0xFF4CAF50.toInt()
    private val colorUploadInitial = 0xFFBDBDBD.toInt()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        setContentView(R.layout.activity_main)

        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)
        btnLogout = findViewById(R.id.btnLogout)
        switchModel = findViewById(R.id.switchModel)
        lblModel = findViewById(R.id.lblModel)

        // --- SkinView 初期化 ---
        val container = findViewById<FrameLayout>(R.id.skinContainer)
        skinView = SkinView3DSurfaceView(this)
        skinView.setEGLContextClientVersion(2)
        skinView.setPreserveEGLContextOnPause(true)

        container.addView(
            skinView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        setupUI()
        checkLogin()

        // GLThread 準備後にスキン読み込み
        skinView.post { loadAccountSkinOrTest() }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun setupUI() {
        btnSelect.backgroundTintList = ColorStateList.valueOf(colorSelect)
        btnSelect.text = "画像を選択"

        btnUpload.visibility = View.GONE
        btnUpload.backgroundTintList = ColorStateList.valueOf(colorUploadInitial)
        btnUpload.text = "アップロード"

        // 0.1.3では自動でスリム/標準判定されるためスイッチは表示しない or 無効化
        switchModel.visibility = View.GONE
        lblModel.visibility = View.GONE

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
            safeRender(createRedTestBitmap())
            return
        }

        scope.launch {
            try {
                val conn = (URL("https://api.minecraftservices.com/minecraft/profile").openConnection()
                        as HttpURLConnection)
                conn.requestMethod = "GET"
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                if (conn.responseCode == 200) {
                    val json = JSONObject(conn.inputStream.bufferedReader().readText())
                    conn.disconnect()

                    val skins = json.optJSONArray("skins")
                    if (skins != null && skins.length() > 0) {
                        val skinUrl = skins.getJSONObject(0).getString("url")
                            .replace("http://", "https://")
                        val stream = URL(skinUrl).openStream()
                        val bmp = BitmapFactory.decodeStream(stream)
                        val fixed = Bitmap.createScaledBitmap(bmp, 64, 64, true)

                        currentSkinBitmap = fixed

                        withContext(Dispatchers.Main) {
                            safeRender(fixed)
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "loadAccountSkinOrTest failed: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                safeRender(createRedTestBitmap())
            }
        }
    }

    private fun createRedTestBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(0xFFFF0000.toInt())
        currentSkinBitmap = bmp
        return bmp
    }

    private fun safeRender(bitmap: Bitmap) {
        skinView.post {
            try {
                skinView.render(bitmap)
                Log.d(TAG, "safeRender: success")
            } catch (e: Exception) {
                Log.e(TAG, "safeRender failed: ${e.message}")
            }
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
                val bmp = Bitmap.createScaledBitmap(
                    orig.copy(Bitmap.Config.ARGB_8888, true),
                    64, 64, true
                )

                currentSkinBitmap = bmp
                hasSelectedSkin = true

                safeRender(bmp)

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
        val token = prefs.getString("minecraft_token", null)
        val bmp = currentSkinBitmap ?: return
        if (token == null) return

        val dialog = AlertDialog.Builder(this)
            .setMessage("アップロード中…")
            .setCancelable(false)
            .show()

        Thread {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("https://api.minecraftservices.com/minecraft/profile/skins")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer $token")
                val boundary = "----SkinBoundary-${System.currentTimeMillis()}"
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

                val os = DataOutputStream(conn.outputStream)
                val line = "\r\n"

                os.writeBytes("--$boundary$line")
                os.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"$line")
                os.writeBytes("Content-Type: image/png$line$line")

                val baos = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
                os.write(baos.toByteArray())

                os.writeBytes(line + "--$boundary--$line")
                os.flush()

                val code = conn.responseCode
                Log.d(TAG, "Upload finished with code: $code")
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed: ${e.message}")
            } finally {
                runOnUiThread { dialog.dismiss() }
                conn?.disconnect()
            }
        }.start()
    }
}
