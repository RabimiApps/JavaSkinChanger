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
import androidx.appcompat.widget.SwitchCompat
import dev.storeforminecraft.skinviewandroid.library.threedimension.ui.SkinView3DSurfaceView
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var skinView: SkinView3DSurfaceView
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

    // 色: 初期の水色(選択ボタン) と アップロードの緑
    private val colorSelect = Color.parseColor("#4FC3F7") // 水色
    private val colorUploadTarget = Color.parseColor("#4CAF50") // 緑
    private val colorUploadInitial = Color.parseColor("#BDBDBD") // アップロード初期色（灰）

    // 選択フラグ
    private var hasSelectedSkin = false

    // モデル (classic = Steve, slim = Alex)
    private var skinVariant: String = "classic"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // SkinView3D を動的に生成して配置（LayoutParams を明示）
        val skinContainer = findViewById<FrameLayout>(R.id.skinContainer)
        skinView = SkinView3DSurfaceView(this)
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        skinContainer.addView(skinView, lp)

        // Z-order を試しに設定（他の View に隠れる場合に調整）
        try {
            skinView.setZOrderOnTop(false)
            skinView.setZOrderMediaOverlay(true)
        } catch (_: Exception) {}

        // Views
        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)
        btnLogout = findViewById(R.id.btnLogout)
        switchModel = findViewById(R.id.switchModel)
        lblModel = findViewById(R.id.lblModel)

        // 初期 UI セットアップ
        btnSelect.backgroundTintList = ColorStateList.valueOf(colorSelect)
        btnSelect.text = "画像を選択"
        btnSelect.isAllCaps = false

        // アップロードは初期時は見えない（後で表示）
        btnUpload.visibility = View.GONE
        btnUpload.backgroundTintList = ColorStateList.valueOf(colorUploadInitial)
        btnUpload.isAllCaps = false
        btnUpload.text = "アップロード"

        // スキンライブラリを紫、ログアウトを赤
        btnLibrary.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9C27B0"))
        btnLibrary.isAllCaps = false
        btnLogout.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F44336"))
        btnLogout.isAllCaps = false

        // スイッチ初期状態: OFF = Steve (classic)
        switchModel.isChecked = false
        lblModel.text = "モデル: Steve"

        // スイッチ色調整（簡易）
        try {
            val thumbStates = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            val thumbColors = intArrayOf(Color.WHITE, Color.WHITE)
            val trackColors = intArrayOf(colorUploadTarget, Color.parseColor("#D0D0D0"))
            switchModel.thumbTintList = ColorStateList(thumbStates, thumbColors)
            switchModel.trackTintList = ColorStateList(thumbStates, trackColors)
        } catch (_: Exception) {}

        // スイッチの変更を反映（Steve <-> Alex）
        switchModel.setOnCheckedChangeListener { _, isChecked ->
            skinVariant = if (isChecked) "slim" else "classic"
            lblModel.text = if (isChecked) "モデル: Alex" else "モデル: Steve"

            // 既に選択されたスキンがあれば preview として再レンダリング
            currentSkinBitmap?.let { bmp ->
                applyVariantToSkinView()
                if (skinView.holder.surface.isValid) {
                    try { skinView.render(bmp) } catch (e: Exception) { e.printStackTrace() }
                } else { pendingBitmap = bmp }
            }
        }

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
            // 常に選択画面を開く（選択後は上書きできる）
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(intent, "スキンを選択"), REQUEST_SKIN_PICK)
        }

        // アップロードボタン
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

        // Surface 準備
        skinView.holder.addCallback(object : android.view.SurfaceHolder.Callback {
            override fun surfaceCreated(holder: android.view.SurfaceHolder) {
                pendingBitmap?.let {
                    try {
                        applyVariantToSkinView()
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

                    // SkinView に variant を反映してレンダー
                    applyVariantToSkinView()
                    if (skinView.holder.surface.isValid) {
                        try {
                            skinView.render(resized)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        pendingBitmap = resized
                    }

                    // 選択済みにして Upload ボタンを表示・アニメーション
                    if (!hasSelectedSkin) {
                        hasSelectedSkin = true
                        // Upload ボタンを表示し、水色の Select ボタンはそのままにする
                        btnUpload.visibility = View.VISIBLE
                        animateButtonToColor(btnUpload, colorUploadInitial, colorUploadTarget)
                    } else {
                        // すでに選択済みでも、Upload ボタンが隠れていれば表示する
                        if (btnUpload.visibility != View.VISIBLE) {
                            btnUpload.visibility = View.VISIBLE
                            btnUpload.backgroundTintList = ColorStateList.valueOf(colorUploadTarget)
                        }
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

    // SkinView に variant を渡す（ライブラリの API に応じて複数パターンを試す）
    private fun applyVariantToSkinView() {
        try {
            val m = skinView.javaClass.getMethod("setVariant", String::class.java)
            m.invoke(skinView, skinVariant)
            return
        } catch (_: NoSuchMethodException) {}
        try {
            val m2 = skinView.javaClass.getMethod("setSlim", java.lang.Boolean.TYPE)
            m2.invoke(skinView, skinVariant == "slim")
            return
        } catch (_: NoSuchMethodException) {}
        // ライブラリ側で別 API の場合はここに追記してください
    }

    // ボタンを色アニメーションで変化させる（ArgbEvaluator を利用）
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
                // 確実に最終色をセット
                button.backgroundTintList = ColorStateList.valueOf(toColor)
            }
            override fun onAnimationCancel(animation: Animator) { button.isEnabled = true }
            override fun onAnimationRepeat(animation: Animator) {}
        })
        anim.start()
    }

    // アップロード処理（Minecraft API へ multipart POST）
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