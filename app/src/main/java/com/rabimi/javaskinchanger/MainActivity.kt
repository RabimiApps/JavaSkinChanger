package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.*
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

    // surface が描画可能か
    private var surfaceReady = false

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

        // SkinView をコードで生成してコンテナに追加（XML に置く場合は findViewById でも可）
        val container = findViewById<FrameLayout>(R.id.skinContainer)
        skinView = SkinView3DSurfaceView(this)
        container.addView(skinView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Surface の準備完了を監視（ここで onResume/onPause を行う）
        skinView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG, "surfaceCreated")
                surfaceReady = true
                // GLSurfaceView の GLThread が未生成だと例外になる場合があるので安全に呼ぶ
                safeCallGLResume()
                // pending があれば描画
                pendingBitmap?.let { safeRender(it) }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.d(TAG, "surfaceChanged: ${width}x$height")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(TAG, "surfaceDestroyed")
                surfaceReady = false
                safeCallGLPause()
            }
        })

        setupUI()
        checkLogin()
        loadAccountSkinOrTest()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        // activity の onResume で無条件に皮を呼ぶと GLSurfaceView の内部状態次第で NPE になる端末があるため、
        // surface が既に出来ていれば安全に呼ぶ、そうでなければ surfaceCreated 側で呼ばせる
        if (surfaceReady) safeCallGLResume()
        // pending があれば描画予約（safeRender 内で surfaceReady チェック）
        pendingBitmap?.let { safeRender(it) }
    }

    override fun onPause() {
        Log.d(TAG, "onPause called")
        // surfaceDestroyed 側でも pause するので try/catch で安全に呼ぶ
        safeCallGLPause()
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

            // 現在表示中のスキンがあればすぐに再描画（スリム/クラシック切替）
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
            loadFallbackSkin()
            return
        }

        scope.launch {
            try {
                val conn = (URL("https://api.minecraftservices.com/minecraft/profile").openConnection() as HttpURLConnection)
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.requestMethod = "GET"
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
                        // ユーザーがまだ選択していなければ pending としてロード（選択済なら無視）
                        if (!hasSelectedSkin) pendingBitmap = scaled

                        withContext(Dispatchers.Main) {
                            // surfaceReady が true なら即描画
                            if (surfaceReady) {
                                safeRender(if (hasSelectedSkin) currentSkinBitmap!! else scaled)
                            }
                        }
                        conn.disconnect()
                        return@launch
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "loadAccountSkinOrTest failed: ${e.message}")
            }

            // 失敗時 / スキン無し
            withContext(Dispatchers.Main) { if (surfaceReady) loadFallbackSkin() else pendingBitmap = createRedTestBitmap() }
        }
    }

    private fun loadFallbackSkin() {
        val bmp = createRedTestBitmap()
        currentSkinBitmap = bmp
        if (!hasSelectedSkin) pendingBitmap = bmp
        if (surfaceReady) safeRender(bmp)
    }

    private fun createRedTestBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(0xFFFF0000.toInt())
        return bmp
    }

    /**
     * 描画は surfaceReady を必ずチェックして行う。
     * surfaceReady=false の場合は pendingBitmap に保留する。
     */
    private fun safeRender(bitmap: Bitmap) {
        if (!surfaceReady) {
            pendingBitmap = bitmap
            return
        }

        skinView.post {
            try {
                applyVariant()
                skinView.render(bitmap)
                pendingBitmap = null
                Log.d(TAG, "safeRender: success")
            } catch (e: Exception) {
                Log.e(TAG, "safeRender failed: ${e.message}")
            }
        }
    }

    private fun applyVariant() {
        try {
            val m = skinView.javaClass.getMethod("setVariant", String::class.java)
            val toApply = pendingVariant ?: skinVariant
            m.invoke(skinView, toApply)
            pendingVariant = null
            Log.d(TAG, "applyVariant applied: $toApply")
        } catch (e: Exception) {
            Log.w(TAG, "applyVariant failed: ${e.message}")
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

    // -----------------------
    // GLSurfaceView の内部 GLThread が存在するかを反射で確認して安全に onResume/onPause を呼ぶ
    // 端末差で GLSurfaceView の内部状態遷移が遅くて null になることがあるための保険。
    // -----------------------
    private fun hasInternalGLThread(): Boolean {
        return try {
            val f = skinView.javaClass.superclass.getDeclaredField("mGLThread")
            f.isAccessible = true
            f.get(skinView) != null
        } catch (_: Exception) {
            // reflection 失敗したら保守的に false にして try/catch 呼び出しに頼る
            false
        }
    }

    private fun safeCallGLResume() {
        try {
            // 反射で null か判定できるなら確認
            if (!hasInternalGLThread()) {
                // まだ内部スレッド無ければ onResume を呼んで作る（try/catch）
                skinView.onResume()
            } else {
                // 既に存在すれば通常呼ぶ
                skinView.onResume()
            }
        } catch (e: Exception) {
            Log.w(TAG, "safeCallGLResume suppressed: ${e.message}")
        }
    }

    private fun safeCallGLPause() {
        try {
            // pause も同様に安全に呼ぶ
            skinView.onPause()
        } catch (e: Exception) {
            Log.w(TAG, "safeCallGLPause suppressed: ${e.message}")
        }
    }
}