package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
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
    private var pendingBitmap: Bitmap? = null
    private var hasSelectedSkin = false

    private var skinVariant: String = "classic"
    private var pendingVariant: String? = null

    private val colorSelect = 0xFF4FC3F7.toInt()
    private val colorUploadTarget = 0xFF4CAF50.toInt()
    private val colorUploadInitial = 0xFFBDBDBD.toInt()

    // coroutine scope for network tasks
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        setContentView(R.layout.activity_main)

        // view binding
        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)
        btnLogout = findViewById(R.id.btnLogout)
        switchModel = findViewById(R.id.switchModel)
        lblModel = findViewById(R.id.lblModel)

        // create SkinView and configure EGL safely if possible
        val container = findViewById<FrameLayout>(R.id.skinContainer)
        skinView = SkinView3DSurfaceView(this)

        try {
            if (skinView is GLSurfaceView) {
                val gl = skinView as GLSurfaceView
                gl.setEGLContextClientVersion(2)
                // try to preserve context to reduce reinit cost on pause/resume
                gl.setPreserveEGLContextOnPause(true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "EGL setup skipped: ${e.message}")
        }

        // add to layout
        container.addView(skinView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        setupUI()
        checkLogin()
        loadAccountSkinOrTest()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        // Let GLSurfaceView manage its GLThread; call its lifecycle methods here.
        try {
            skinView.onResume()
        } catch (e: Exception) {
            Log.w(TAG, "skinView.onResume suppressed: ${e.message}")
        }

        // If there's a pending bitmap (loaded before GL ready), render it now.
        pendingBitmap?.let {
            safeRender(it)
        }
    }

    override fun onPause() {
        Log.d(TAG, "onPause called")
        // Pause GL first, then super
        try {
            skinView.onPause()
        } catch (e: Exception) {
            Log.w(TAG, "skinView.onPause suppressed: ${e.message}")
        }
        super.onPause()
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

        switchModel.setOnCheckedChangeListener { _, isChecked ->
            val newVariant = if (isChecked) "slim" else "classic"
            skinVariant = newVariant
            lblModel.text = if (isChecked) "モデル: Alex" else "モデル: Steve"

            currentSkinBitmap?.let {
                pendingVariant = skinVariant
                safeRender(it)
            }
        }

        btnSelect.setOnClickListener { selectSkinImage() }
        btnUpload.setOnClickListener { handleUpload() }
        btnLibrary.setOnClickListener { AlertDialog.Builder(this).setMessage("未実装").setPositiveButton("OK", null).show() }
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

    /**
     * 起動時にアカウントスキンを取得して表示する（トークンがあれば）。
     * ユーザーが選択済み (hasSelectedSkin==true) の場合はアカウントスキンを pending にしない。
     */
    private fun loadAccountSkinOrTest() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val token = prefs.getString("minecraft_token", null)
        if (token == null) {
            // no token -> fallback
            pendingBitmap = createRedTestBitmap()
            return
        }

        scope.launch {
            try {
                val conn = (URL("https://api.minecraftservices.com/minecraft/profile").openConnection() as HttpURLConnection)
                conn.setRequestMethod("GET")
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000

                val code = conn.responseCode
                if (code == 200) {
                    val body = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(body)
                    val skins = json.optJSONArray("skins")
                    if (skins != null && skins.length() > 0) {
                        val skinUrl = skins.getJSONObject(0).getString("url")
                        val bmpStream = URL(skinUrl).openStream()
                        val bmp = BitmapFactory.decodeStream(bmpStream)
                        val scaled = Bitmap.createScaledBitmap(bmp, 64, 64, true)
                        currentSkinBitmap = scaled
                        if (!hasSelectedSkin) pendingBitmap = scaled

                        withContext(Dispatchers.Main) {
                            // try to render now if GL ready (safeRender will post to skinView)
                            safeRender(if (hasSelectedSkin) currentSkinBitmap!! else scaled)
                        }
                        conn.disconnect()
                        return@launch
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "loadAccountSkinOrTest failed: ${e.message}")
            }

            // fallback
            withContext(Dispatchers.Main) {
                pendingBitmap = createRedTestBitmap()
                safeRender(pendingBitmap!!)
            }
        }
    }

    private fun createRedTestBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(0xFFFF0000.toInt())
        currentSkinBitmap = bmp
        return bmp
    }

    /**
     * 描画。skinView の GLThread に post して実行するので安全。
     * GL がまだ初期化されていない場合は GLSurfaceView が自動的に処理することが多いが、
     * 念のため pendingBitmap に保持して onResume 時に再描画する。
     */
    // pendingBitmap と pendingVariant はクラス変数として保持
private var pendingBitmap: Bitmap? = null
private var pendingVariant: String? = null

// 安全にレンダリング
private fun safeRender(bitmap: Bitmap) {
    pendingBitmap = bitmap

    if (skinView == null) {
        Log.w(TAG, "safeRender postponed: skinView is null")
        return
    }

    skinView.post {
        val view = skinView ?: run {
            Log.w(TAG, "safeRender suppressed: skinView became null")
            return@post
        }

        try {
            applyVariant()
            view.render(bitmap)
            pendingBitmap = null
            Log.d(TAG, "safeRender: success")
        } catch (e: Exception) {
            Log.e(TAG, "safeRender failed: ${e.message}")
        }
    }
}

// variant を安全に適用
private fun applyVariant() {
    val view = skinView ?: run {
        Log.w(TAG, "applyVariant skipped: skinView is null")
        return
    }

    val variant = pendingVariant ?: skinVariant ?: run {
        Log.w(TAG, "applyVariant skipped: variant is null")
        return
    }

    try {
        val m = view.javaClass.getMethod("setVariant", String::class.java)
        m.invoke(view, variant)
        pendingVariant = null
        Log.d(TAG, "applyVariant applied: $variant")
    } catch (e: Exception) {
        Log.w(TAG, "applyVariant failed: ${e.message}")
    }
}

// onResume 内で pendingBitmap があれば自動再描画
fun onResumeSafe() {
    Log.d(TAG, "onResumeSafe called")
    val view = skinView
    if (view == null) {
        Log.w(TAG, "onResumeSafe skipped: skinView is null")
        return
    }

    try {
        // GLSurfaceView の onResume を安全に呼ぶ
        view.onResume()
    } catch (e: Exception) {
        Log.w(TAG, "skinView.onResume suppressed: ${e.message}")
    }

    // pendingBitmap があれば再描画
    pendingBitmap?.let { bitmap ->
        Log.d(TAG, "onResumeSafe: re-render pending bitmap")
        safeRender(bitmap)
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
                pendingBitmap = bmp
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
                os.writeBytes("Content-Disposition: form-data; name=\"variant\"$line$line")
                os.writeBytes("$skinVariant$line")

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