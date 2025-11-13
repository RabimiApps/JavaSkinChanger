package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import dev.storeforminecraft.skinviewandroid.library.threedimension.ui.SkinView3DSurfaceView
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SkinDebug"
    }

    private lateinit var skinView: SkinView3DSurfaceView
    private lateinit var skinContainer: FrameLayout
    private lateinit var txtUsername: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnLibrary: Button
    private lateinit var btnLogout: Button
    private lateinit var switchModel: SwitchCompat
    private lateinit var lblModel: TextView

    private val REQUEST_SKIN_PICK = 1001
    private var pendingBitmap: Bitmap? = null
    private var currentSkinBitmap: Bitmap? = null
    private var hasSelectedSkin = false
    private var skinVariant: String = "classic"

    private val colorSelect = Color.parseColor("#4FC3F7")
    private val colorUploadTarget = Color.parseColor("#4CAF50")
    private val colorUploadInitial = Color.parseColor("#BDBDBD")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        skinContainer = findViewById(R.id.skinContainer)
        skinView = SkinView3DSurfaceView(this)

        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        skinContainer.addView(skinView, lp)

        // Surface ができたら必ず描画
        skinView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG, "surfaceCreated: isValid=${holder.surface.isValid}")
                val bmpToRender = pendingBitmap ?: createTestBitmap(64, 64)
                try {
                    applyVariantToSkinView()
                    skinView.render(bmpToRender)
                    pendingBitmap = null
                    Log.d(TAG, "rendered bitmap in surfaceCreated")
                } catch (e: Exception) {
                    Log.e(TAG, "render failed: ${e.message}")
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.d(TAG, "surfaceChanged: format=$format size=${width}x${height}")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(TAG, "surfaceDestroyed")
            }
        })

        // UI 初期化
        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)
        btnLogout = findViewById(R.id.btnLogout)
        switchModel = findViewById(R.id.switchModel)
        lblModel = findViewById(R.id.lblModel)

        btnSelect.backgroundTintList = ColorStateList.valueOf(colorSelect)
        btnSelect.isAllCaps = false
        btnSelect.text = "画像を選択"
        btnUpload.visibility = View.GONE
        btnUpload.backgroundTintList = ColorStateList.valueOf(colorUploadInitial)
        btnUpload.isAllCaps = false
        btnUpload.text = "アップロード"
        btnLibrary.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9C27B0"))
        btnLibrary.isAllCaps = false
        btnLogout.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
        btnLogout.isAllCaps = false
        switchModel.isChecked = false
        lblModel.text = "モデル: Steve"

        // モデル切替
        switchModel.setOnCheckedChangeListener { _, isChecked ->
            skinVariant = if (isChecked) "slim" else "classic"
            lblModel.text = if (isChecked) "モデル: Alex" else "モデル: Steve"
            currentSkinBitmap?.let { bmp ->
                pendingBitmap = bmp
                if (skinView.holder.surface.isValid) {
                    try {
                        applyVariantToSkinView()
                        skinView.render(bmp)
                    } catch (_: Exception) {}
                    pendingBitmap = null
                }
            }
        }

        // Minecraft 認証チェック
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val username = prefs.getString("minecraft_username", null)
        val token = prefs.getString("minecraft_token", null)
        if (username.isNullOrBlank() || token.isNullOrBlank()) {
            AlertDialog.Builder(this)
                .setTitle("ログインが必要です")
                .setMessage("ログイン情報が見つかりません。再ログインしてください。")
                .setPositiveButton("OK") { _, _ ->
                    startActivity(Intent(this, WelcomeActivity::class.java))
                    finish()
                }
                .setCancelable(false)
                .show()
            return
        }
        txtUsername.text = "ログイン中: $username"

        // 画像選択
        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(intent, "スキンを選択"), REQUEST_SKIN_PICK)
        }

        btnUpload.setOnClickListener { handleUpload() }
        btnLibrary.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("ライブラリ")
                .setMessage("スキンライブラリ機能はまだ実装されていません")
                .setPositiveButton("OK", null)
                .show()
        }
        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        try { skinView.onResume() } catch (_: Exception) {}
    }

    override fun onPause() {
        try { skinView.onPause() } catch (_: Exception) {}
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SKIN_PICK && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val bmpOriginal = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    val bmp = Bitmap.createScaledBitmap(bmpOriginal.copy(Bitmap.Config.ARGB_8888, true), 64, 64, true)
                    currentSkinBitmap = bmp
                    pendingBitmap = bmp
                    if (skinView.holder.surface.isValid) {
                        applyVariantToSkinView()
                        skinView.render(bmp)
                        pendingBitmap = null
                    }
                    if (!hasSelectedSkin) {
                        hasSelectedSkin = true
                        btnUpload.visibility = View.VISIBLE
                        btnUpload.backgroundTintList = ColorStateList.valueOf(colorUploadTarget)
                    }
                } catch (e: Exception) {
                    AlertDialog.Builder(this)
                        .setTitle("エラー")
                        .setMessage("スキンの読み込みに失敗しました: ${e.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    private fun applyVariantToSkinView() {
        try {
            val m = skinView.javaClass.getMethod("setVariant", String::class.java)
            m.invoke(skinView, skinVariant)
        } catch (_: Exception) {}
    }

    private fun createTestBitmap(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        val cell = w / 8
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                paint.color = if ((x + y) % 2 == 0) Color.LTGRAY else Color.DKGRAY
                canvas.drawRect((x*cell).toFloat(), (y*cell).toFloat(), ((x+1)*cell).toFloat(), ((y+1)*cell).toFloat(), paint)
            }
        }
        paint.color = Color.MAGENTA
        canvas.drawCircle((w*0.75).toFloat(), (h*0.25).toFloat(), (w*0.08).toFloat(), paint)
        return bmp
    }

    private fun handleUpload() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val token = prefs.getString("minecraft_token", null)
        val bitmap = currentSkinBitmap
        if (bitmap == null || token.isNullOrBlank()) return
        val baos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.PNG, 100, baos)
        val imageBytes = baos.toByteArray()

        val progress = ProgressBar(this)
        val dialog = AlertDialog.Builder(this)
            .setTitle("アップロード中")
            .setView(progress)
            .setCancelable(false)
            .create()
        dialog.show()

        Thread {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("https://api.minecraftservices.com/minecraft/profile/skins")
                conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Authorization", "Bearer $token")
                val boundary = "----SkinUploadBoundary${System.currentTimeMillis()}"
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                val out = DataOutputStream(conn.outputStream)
                val lineEnd = "\r\n"
                val twoHyphens = "--"
                out.writeBytes(twoHyphens + boundary + lineEnd)
                out.writeBytes("Content-Disposition: form-data; name=\"variant\"$lineEnd$lineEnd")
                out.writeBytes("$skinVariant$lineEnd")
                out.writeBytes(twoHyphens + boundary + lineEnd)
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"$lineEnd")
                out.writeBytes("Content-Type: image/png$lineEnd$lineEnd")
                out.write(imageBytes)
                out.writeBytes(lineEnd + twoHyphens + boundary + twoHyphens + lineEnd)
                out.flush()
                out.close()
                val code = conn.responseCode
                runOnUiThread { dialog.dismiss() }
            } catch (_: Exception) {
                runOnUiThread { dialog.dismiss() }
            } finally {
                conn?.disconnect()
            }
        }.start()
    }
}
