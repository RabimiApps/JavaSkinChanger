package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
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

        // 初期色をセット（選択ボタンを水色に）
        btnSelect.backgroundTintList = ColorStateList.valueOf(colorInitial)
        btnSelect.text = "画像を選択"
        btnSelect.isAllCaps = false

        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val username = prefs.getString("minecraft_username", null)
        val token = prefs.getString("minecraft_token", null)

        // 🔸 ログインチェック
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

        // 🔹 スキン選択（ギャラリー）
        btnSelect.setOnClickListener {
            if (isUploadState) {
                // アップロード状態ならアップロード処理へ
                handleUpload()
                return@setOnClickListener
            }
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            startActivityForResult(Intent.createChooser(intent, "スキンを選択"), REQUEST_SKIN_PICK)
        }

        // 🔹 アップロード（現在はダイアログでプレースホルダ）
        btnUpload.setOnClickListener {
            handleUpload()
        }

        // 🔹 スキンライブラリ
        btnLibrary.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("ライブラリ")
                .setMessage("スキンライブラリ機能はまだ実装されていません")
                .setPositiveButton("OK", null)
                .show()
        }

        // 🔹 ログアウト
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
                    val resized = if (bitmap.width != 64 || bitmap.height != 64)
                        Bitmap.createScaledBitmap(bitmap, 64, 64, true)
                    else bitmap

                    skinImage.setImageBitmap(resized)

                    if (skinView.holder.surface.isValid) skinView.render(resized)
                    else pendingBitmap = resized

                    // 画像選択が成功したので、ボタンをアニメーションで水色->緑にして
                    // テキストを「アップロード」に変える
                    if (!isUploadState) animateSelectButtonToUpload()

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
                // 同じアップロード処理を呼べるように、既存のアップロードボタンは非表示にする（任意）
                btnUpload.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator) {
                btnSelect.isEnabled = true
            }

            override fun onAnimationRepeat(animation: Animator) {}
        })
        colorAnimation.start()
    }

    // アップロード処理（現在はプレースホルダ）
    private fun handleUpload() {
        // ここに実際のアップロード処理を実装してください。
        // 現在はプレースホルダのダイアログ表示のみ。
        AlertDialog.Builder(this)
            .setTitle("アップロード")
            .setMessage("アップロード処理はまだ実装されていません")
            .setPositiveButton("OK", null)
            .show()
    }
}
