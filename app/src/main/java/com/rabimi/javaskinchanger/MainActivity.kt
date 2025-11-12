package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
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

    // 現在表示しているスキンのビットマップ（アップロード時に使用）
    private var currentSkinBitmap: Bitmap? = null

    // 色
    private val colorSelect = Color.parseColor("#4FC3F7")
    private val colorUploadTarget = Color.parseColor("#4CAF50")
    private val colorUploadInitial = Color.parseColor("#BDBDBD")

    private var hasSelectedSkin = false
    private var skinVariant: String = "classic"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // skinContainer を先に取得
        skinContainer = findViewById(R.id.skinContainer)

        // SkinView を動的に生成して追加（XML で直接置かない）
        skinView = SkinView3DSurfaceView(this)
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        // 必要ならマージンや重なり調整もここで設定可能
        skinContainer.addView(skinView, lp)

        // デバッグ: 背景を付けて見えるか確認しやすくする（不要なら削除）
        try {
            skinView.setBackgroundColor(Color.parseColor("#EEEEEE"))
        } catch (_: Exception) {}

        // Views
        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)
        btnLogout = findViewById(R.id.btnLogout)
        switchModel = findViewById(R.id.switchModel)
        lblModel = findViewById(R.id.lblModel)

        // UI 初期化
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

        // skinContainer / skinView サイズ確認ログ
        skinContainer.post {
            Log.d(TAG, "skinContainer size: ${skinContainer.width}x${skinContainer.height}")
            Log.d(TAG, "skinView size: ${skinView.width}x${skinView.height}, visible=${skinView.visibility}")
        }

        // skinView メソッド一覧（variant API 探し用）
        try {
            val methods = skinView.javaClass.methods
            val names = methods.map { it.name }.distinct().sorted().joinToString(", ")
            Log.d(TAG, "SkinView methods: $names")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to list SkinView methods: ${e.message}")
        }

        // スイッチ処理
        switchModel.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "switchModel changed: isChecked=$isChecked")
            if (isChecked) {
                skinVariant = "slim"
                lblModel.text = "モデル: Alex"
            } else {
                skinVariant = "classic"
                lblModel.text = "モデル: Steve"
            }

            currentSkinBitmap?.let { bmp ->
                applyVariantToSkinView()
                if (skinView.holder.surface.isValid) {
                    try {
                        skinView.render(bmp)
                        Log.d(TAG, "render called immediately after switch")
                    } catch (e: Exception) {
                        Log.e(TAG, "render failed after switch: ${e.message}")
                    }
                } else {
                    pendingBitmap = bmp
                    Log.d(TAG, "surface not valid, stored to pendingBitmap")
                }
            }
        }

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

        // Surface のコールバックにログを追加
        skinView.holder.addCallback(object : android.view.SurfaceHolder.Callback {
            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                Log.d(TAG, "surfaceCreated: isValid=${holder.surface.isValid}")
                if (pendingBitmap != null) {
                    val pb = pendingBitmap
                    if (pb != null) {
                        try {
                            applyVariantToSkinView()
                            skinView.render(pb)
                            Log.d(TAG, "rendered pending bitmap in surfaceCreated")
                        } catch (e: Exception) {
                            Log.e(TAG, "render failed in surfaceCreated: ${e.message}")
                        }
                        pendingBitmap = null
                    }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SKIN_PICK && resultCode == Activity.RESULT_OK) {
            if (data != null && data.data != null) {
                val uri = data.data
                try {
                    val bitmapOriginal: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    val bitmap = bitmapOriginal.copy(Bitmap.Config.ARGB_8888, true)
                    val resized: Bitmap
                    if (bitmap.width != 64 || bitmap.height != 64) {
                        resized = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
                    } else {
                        resized = bitmap
                    }

                    currentSkinBitmap = resized
                    Log.d(TAG, "selected skin size: ${resized.width}x${resized.height}")

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
                        Log.d(TAG, "surface not valid, stored to pendingBitmap")
                    }

                    if (!hasSelectedSkin) {
                        hasSelectedSkin = true
                        btnUpload.visibility = View.VISIBLE
                        animateButtonToColor(btnUpload, colorUploadInitial, colorUploadTarget)
                    } else {
                        if (btnUpload.visibility != View.VISIBLE) {
                            btnUpload.visibility = View.VISIBLE
                            btnUpload.backgroundTintList = ColorStateList.valueOf(colorUploadTarget)
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load selected image: ${e.message}")
                    e.printStackTrace()
                    AlertDialog.Builder(this)
                        .setTitle("エラー")
                        .setMessage("スキンの読み込みに失敗しました")
                        .setPositiveButton("OK", null)
                        .show()
                }
            } else {
                AlertDialog.Builder(this)
                    .setTitle("エラー")
                    .setMessage("画像が選択されませんでした。")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun applyVariantToSkinView() {
        try {
            val m = skinView.javaClass.getMethod("setVariant", String::class.java)
            m.invoke(skinView, skinVariant)
            Log.d(TAG, "Invoked setVariant($skinVariant)")
            return
        } catch (_: NoSuchMethodException) {
            // continue
        } catch (e: Exception) {
            Log.w(TAG, "setVariant invoke error: ${e.message}")
        }
        try {
            val m2 = skinView.javaClass.getMethod("setSlim", java.lang.Boolean.TYPE)
            m2.invoke(skinView, skinVariant == "slim")
            Log.d(TAG, "Invoked setSlim(${skinVariant == "slim"})")
            return
        } catch (_: NoSuchMethodException) {
            // no-op
        } catch (e: Exception) {
            Log.w(TAG, "setSlim invoke error: ${e.message}")
        }
        // ここに別の API 名があれば追記
    }

    private fun animateButtonToColor(button: Button, fromColor: Int, toColor: Int) {
        button.isEnabled = false
        val anim = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        anim.duration = 380L
        anim.addUpdateListener { a ->
            val color = a.animatedValue as Int
            button.backgroundTintList = ColorStateList.valueOf(color)
        }
        anim.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                button.isEnabled = true
                button.backgroundTintList = ColorStateList.valueOf(toColor)
            }
            override fun onAnimationCancel(animation: Animator) {
                button.isEnabled = true
            }
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
                e.printStackTrace()
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
}