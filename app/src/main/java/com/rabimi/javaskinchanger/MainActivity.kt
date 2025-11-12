package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.animation.Animator
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import dev.storeforminecraft.skinviewandroid.library.threedimension.ui.SkinView3DSurfaceView
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var skinView: SkinView3DSurfaceView
    private lateinit var skinImage: ImageView
    private lateinit var txtUsername: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnLibrary: Button
    private lateinit var btnLogout: Button

    private val REQUEST_SKIN_PICK = 1001
    private var pendingBitmap: Bitmap? = null

    // 現在表示しているスキンのビットマップ（アップロード時に使用）
    private var currentSkinBitmap: Bitmap? = null

    // 色: 初期の水色と選択後の緑
    private val colorInitial = Color.parseColor("#4FC3F7") // 水色
    private val colorSelected = Color.parseColor("#4CAF50") // 緑

    // 選択→アップロード状態かどうか
    private var isUploadState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ✅ SkinView3Dを動的に生成して配置
        val skinContainer = findViewById<FrameLayout>(R.id.skinContainer)
        skinView = SkinView3DSurfaceView(this)
        skinContainer.addView(skinView)

        skinImage = findViewById(R.id.skinImage)
        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)
        btnLogout = findViewById(R.id.btnLogout)

        // 初期 UI セットアップ
        btnSelect.backgroundTintList = ColorStateList.valueOf(colorInitial)
        btnSelect.text = "画像を選択"
        btnSelect.isAllCaps = false

        // アップロードは初期時非表示（要望）
        btnUpload.visibility = View.GONE

        // スキンライブラリを紫に変更（要望）
        btnLibrary.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9C27B0")) // 紫
        btnLibrary.isAllCaps = false

        // ログアウトを赤（既に赤の場合はそのまま）
        btnLogout.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336")) // 赤
        btnLogout.isAllCaps = false

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val username = prefs.getString("minecraft_username", null)
        val token = prefs.getString("minecraft_token", null)

        // ログインチェック
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

        // スキン選択（ギャラリー）
        btnSelect.setOnClickListener {
            if (isUploadState) {
                // ラベルが「アップロード」になっている場合はアップロード処理
                handleUpload()
                return@setOnClickListener
            } else {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
                startActivityForResult(Intent.createChooser(intent, "スキンを選択"), REQUEST_SKIN_PICK)
            }
        }

        // btnUpload は初期は非表示のままだが、念のためハンドラは残す
        btnUpload.setOnClickListener {
            handleUpload()
        }

        // スキンライブラリ
        btnLibrary.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("ライブラリ")
                .setMessage("スキンライブラリ機能はまだ実装されていません")
                .setPositiveButton("OK", null)
                .show()
        }

        // ログアウト
        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

        // Surface準備
        skinView.holder.addCallback(object : android.view.SurfaceHolder.Callback {
            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                pendingBitmap?.let {
                    try {
                        skinView.render(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    pendingBitmap = null
                }
            }

            override fun surfaceChanged(holder: android.view.SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: android.view.SurfaceHolder) {}
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SKIN_PICK && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val bitmapOriginal: Bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    val bitmap = bitmapOriginal.copy(Bitmap.Config.ARGB_8888, true)
                    val resized = if (bitmap.width != 64 || bitmap.height != 64) {
                        Bitmap.createScaledBitmap(bitmap, 64, 64, true)
                    } else {
                        bitmap
                    }

                    // 現在のスキンを保持
                    currentSkinBitmap = resized

                    skinImage.setImageBitmap(resized)

                    if (skinView.holder.surface.isValid) {
                        skinView.render(resized)
                    } else {
                        pendingBitmap = resized
                    }

                    // 画像選択が成功したので、btnSelect をアニメーションで水色->緑にして
                    // テキストを「アップロード」に変える（btnUpload は非表示のまま）
                    if (!isUploadState) {
                        animateSelectButtonToUpload()
                    } else {
                        // no-op
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    AlertDialog.Builder(this)
                        .setTitle("エラー")
                        .setMessage("スキンの読み込みに失敗しました")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }

    // 選択ボタンをアニメーションで水色 -> 緑に変化させ、完了後にアップロード状態にする
    private fun animateSelectButtonToUpload() {
        btnSelect.isEnabled = false

        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorInitial, colorSelected)
        colorAnimation.duration = 420L
        colorAnimation.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            btnSelect.backgroundTintList = ColorStateList.valueOf(color)
        }
        colorAnimation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                isUploadState = true
                btnSelect.text = "アップロード"
                btnSelect.isEnabled = true
                // 要望: btnUpload は表示しない（操作は btnSelect から行う）
                btnUpload.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator) {
                btnSelect.isEnabled = true
            }

            override fun onAnimationRepeat(animation: Animator) {}
        })
        colorAnimation.start()
    }

    // アップロード処理（Minecraft API へ実際に PUT/POST する実装）
    // 注意: prefs に保存されたトークンを使用します。実運用ではトークンの種類や有効性を確認してください。
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

        // プログレス表示（簡易）
        val progressView = ProgressBar(this)
        val dialog = AlertDialog.Builder(this)
            .setTitle("アップロード中")
            .setView(progressView)
            .setCancelable(false)
            .create()
        dialog.show()

        // 画像を PNG に変換
        val baos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.PNG, 100, baos)
        val imageBytes = baos.toByteArray()

        // ネットワークは別スレッドで
        Thread {
            var conn: HttpURLConnection? = null
            try {
                // Minecraft API: POST https://api.minecraftservices.com/minecraft/profile/skins
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

                // variant field (classic or slim). Use classic by default.
                out.writeBytes(twoHyphens + boundary + lineEnd)
                out.writeBytes("Content-Disposition: form-data; name=\"variant\"$lineEnd")
                out.writeBytes("Content-Type: text/plain; charset=UTF-8$lineEnd$lineEnd")
                out.writeBytes("classic$lineEnd")

                // file field
                out.writeBytes(twoHyphens + boundary + lineEnd)
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"skin.png\"$lineEnd")
                out.writeBytes("Content-Type: image/png$lineEnd$lineEnd")
                out.write(imageBytes)
                out.writeBytes(lineEnd)

                // end boundary
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