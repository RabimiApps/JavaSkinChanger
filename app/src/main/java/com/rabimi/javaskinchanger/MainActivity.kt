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
import android.view.View
import android.widget.*
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.animation.Animator
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

    // 色設定
    private val colorSelect = Color.parseColor("#4FC3F7")
    private val colorUploadTarget = Color.parseColor("#4CAF50")
    private val colorUploadInitial = Color.parseColor("#BDBDBD")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI 初期化
        skinContainer = findViewById(R.id.skinContainer)
        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)
        btnLogout = findViewById(R.id.btnLogout)
        switchModel = findViewById(R.id.switchModel)
        lblModel = findViewById(R.id.lblModel)

        btnSelect.backgroundTintList = ColorStateList.valueOf(colorSelect)
        btnSelect.text = "画像を選択"
        btnSelect.isAllCaps = false

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

        // SkinView の生成
        skinView = SkinView3DSurfaceView(this)
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        skinContainer.addView(skinView, lp)

        // 前面化ログ
        try {
            skinView.bringToFront()
            skinView.requestLayout()
            skinView.invalidate()
            Log.d(TAG, "Called bringToFront/requestLayout/invalidate on skinView")
        } catch (e: Exception) {
            Log.w(TAG, "bringToFront/requestLayout failed: ${e.message}")
        }

        // デバッグ: SkinView メソッド一覧
        try {
            val methods = skinView.javaClass.methods
            val names = methods.map { it.name }.distinct().sorted().joinToString(", ")
            Log.d(TAG, "SkinView methods: $names")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to list SkinView methods: ${e.message}")
        }

        // ユーザー情報チェック
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

        // スイッチ処理
        switchModel.setOnCheckedChangeListener { _, isChecked ->
            skinVariant = if (isChecked) {
                lblModel.text = "モデル: Alex"
                "slim"
            } else {
                lblModel.text = "モデル: Steve"
                "classic"
            }
            currentSkinBitmap?.let {
                applyVariantToSkinView()
                if (skinView.holder.surface.isValid) {
                    try {
                        skinView.render(it)
                        Log.d(TAG, "render called immediately after switch")
                    } catch (e: Exception) {
                        Log.e(TAG, "render failed after switch: ${e.message}")
                    }
                } else {
                    pendingBitmap = it
                    Log.d(TAG, "surface not valid, stored to pendingBitmap")
                }
            }
        }

        // ボタン処理
        btnSelect.setOnClickListener { selectSkinImage() }
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

        // SurfaceCallback で pendingBitmap 描画
        skinView.holder.addCallback(object : android.view.SurfaceHolder.Callback {
            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                Log.d(TAG, "surfaceCreated: isValid=${holder.surface.isValid}")
                pendingBitmap?.let { bmp ->
                    applyVariantToSkinView()
                    try {
                        skinView.render(bmp)
                        Log.d(TAG, "rendered pending bitmap in surfaceCreated")
                    } catch (e: Exception) {
                        Log.e(TAG, "render failed in surfaceCreated: ${e.message}")
                    }
                    pendingBitmap = null
                }
            }

            override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {
                Log.d(TAG, "surfaceChanged: format=$format size=${width}x${height} isValid=${holder.surface.isValid}")
            }

            override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {
                Log.d(TAG, "surfaceDestroyed")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        try {
            skinView.onResume()
        } catch (e: Exception) {
            Log.w(TAG, "skinView.onResume() failed: ${e.message}")
        }
    }

    override fun onPause() {
        try {
            skinView.onPause()
        } catch (e: Exception) {
            Log.w(TAG, "skinView.onPause() failed: ${e.message}")
        }
        super.onPause()
    }

    private fun selectSkinImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        startActivityForResult(Intent.createChooser(intent, "スキンを選択"), REQUEST_SKIN_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_SKIN_PICK || resultCode != Activity.RESULT_OK || data?.data == null) return

        val uri = data.data
        try {
            val bitmapOriginal: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val bitmap = bitmapOriginal.copy(Bitmap.Config.ARGB_8888, true)
            val resized = if (bitmap.width != 64 || bitmap.height != 64)
                Bitmap.createScaledBitmap(bitmap, 64, 64, true) else bitmap

            currentSkinBitmap = resized
            applyVariantToSkinView()
            if (skinView.holder.surface.isValid) {
                try {
                    skinView.render(resized)
                    Log.d(TAG, "render called in onActivityResult")
                } catch (e: Exception) {
                    Log.e(TAG, "render failed in onActivityResult: ${e.message}")
                }
            } else {
                pendingBitmap = resized
            }

            if (!hasSelectedSkin) {
                hasSelectedSkin = true
                btnUpload.visibility = View.VISIBLE
                animateButtonToColor(btnUpload, colorUploadInitial, colorUploadTarget)
            } else {
                btnUpload.visibility = View.VISIBLE
                btnUpload.backgroundTintList = ColorStateList.valueOf(colorUploadTarget)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load selected image: ${e.message}")
            AlertDialog.Builder(this)
                .setTitle("エラー")
                .setMessage("スキンの読み込みに失敗しました")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun applyVariantToSkinView() {
        try {
            skinView.javaClass.getMethod("setVariant", String::class.java).invoke(skinView, skinVariant)
            Log.d(TAG, "Invoked setVariant($skinVariant)")
        } catch (_: NoSuchMethodException) {
            try {
                skinView.javaClass.getMethod("setSlim", Boolean::class.javaPrimitiveType).invoke(skinView, skinVariant == "slim")
                Log.d(TAG, "Invoked setSlim(${skinVariant == "slim"})")
            } catch (_: NoSuchMethodException) { /* no-op */ }
        } catch (e: Exception) {
            Log.w(TAG, "setVariant/setSlim invoke error: ${e.message}")
        }
    }

    private fun animateButtonToColor(button: Button, fromColor: Int, toColor: Int) {
        button.isEnabled = false
        val anim = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        anim.duration = 380L
        anim.addUpdateListener { button.backgroundTintList = ColorStateList.valueOf(it.animatedValue as Int) }
        anim.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) { button.isEnabled = true; button.backgroundTintList = ColorStateList.valueOf(toColor) }
            override fun onAnimationCancel(animation: Animator) { button.isEnabled = true }
            override fun onAnimationRepeat(animation: Animator) {}
        })
        anim.start()
    }

    private fun handleUpload() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val token = prefs.getString("minecraft_token", null)
        val bitmap = currentSkinBitmap

        if (bitmap == null) {
            AlertDialog.Builder(this)
                .setTitle("アップロードエラー")
                .setMessage("アップロードするスキンが選択されていません。")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        if (token.isNullOrBlank()) {
            AlertDialog.Builder(this)
                .setTitle("認証エラー")
                .setMessage("Minecraft の認証トークンが見つかりません。再ログインしてください。")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val progressView = ProgressBar(this)
        val dialog = AlertDialog.Builder(this)
            .setTitle("アップロード中")
            .setView(progressView)
            .setCancelable(false)
            .create()
        dialog.show()

        val baos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.PNG, 100, baos)
        val imageBytes = baos.toByteArray()

        Thread {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("https://api.minecraftservices.com/minecraft/profile/skins")
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    doInput = true
                    useCaches = false
                    connectTimeout = 15000
                    readTimeout = 15000
                    setRequestProperty("Authorization", "Bearer $token")
                }

                val boundary = "----SkinUploadBoundary${System.currentTimeMillis()}"
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                val out = DataOutputStream(conn.outputStream)
                val lineEnd = "\r\n"
                val twoHyphens = "--"

                out.writeBytes(twoHyphens + boundary + lineEnd)
                out.writeBytes("Content-Disposition: form-data; name=\"variant\"$lineEnd")
                out.writeBytes("Content-Type: text/plain; charset=UTF-8$lineEnd$lineEnd")
                out.writeBytes("$skinVariant$lineEnd")

                out.writeBytes(twoHyphens + boundary + lineEnd)
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"$lineEnd")
                out.writeBytes("Content-Type: image/png$lineEnd$lineEnd")
                out.write(imageBytes)
                out.writeBytes(lineEnd)

                out.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
                out.flush()
                out.close()

                val responseCode = conn.responseCode
                val responseMessage = conn.responseMessage

                runOnUiThread {
                    dialog.dismiss()
                    if (responseCode in 200..299) {
                        AlertDialog.Builder(this)
                            .setTitle("アップロード完了")
                            .setMessage("スキンのアップロードに成功しました。")
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        AlertDialog.Builder(this)
                            .setTitle("アップロード失敗")
                            .setMessage("HTTP $responseCode: $responseMessage")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    dialog.dismiss()
                    AlertDialog.Builder(this)
                        .setTitle("アップロードエラー")
                        .setMessage("通信中にエラーが発生しました: ${e.message}")
                        .setPositiveButton("OK", null)
                        .show()
                }
            } finally {
                conn?.disconnect()
            }
        }.start()
    }

    // 小さなテスト用ビットマップ
    private fun createTestBitmap(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        val cell = w / 8
        for (y in 0 until 8) for (x in 0 until 8) {
            paint.color = if ((x + y) % 2 == 0) Color.LTGRAY else Color.DKGRAY
            canvas.drawRect((x * cell).toFloat(), (y * cell).toFloat(), ((x + 1) * cell).toFloat(), ((y + 1) * cell).toFloat(), paint)
        }
        paint.color = Color.MAGENTA
        canvas.drawCircle((w * 0.75).toFloat(), (h * 0.25).toFloat(), (w * 0.08).toFloat(), paint)
        return bmp
    }
}
