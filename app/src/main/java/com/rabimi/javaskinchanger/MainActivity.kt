package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import dev.storeforminecraft.skinviewandroid.library.threedimension.ui.SkinView3DSurfaceView
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var skinView3D: SkinView3DSurfaceView
    private lateinit var skinImage: ImageView
    private lateinit var txtUsername: TextView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnLibrary: Button
    private lateinit var btnLogout: Button

    private var selectedSkinUri: Uri? = null

    // 画像選択用
    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedSkinUri = uri
                loadSkinPreview(uri)
                loadSkin3D(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        skinView3D = findViewById(R.id.skinView)
        skinImage = findViewById(R.id.skinImage)
        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnLibrary = findViewById(R.id.btnLibrary)
        btnLogout = findViewById(R.id.btnLogout)

        txtUsername.text = "ログイン中: ユーザー"

        btnSelect.setOnClickListener { openImagePicker() }
        btnUpload.setOnClickListener { uploadSkin() }
        btnLibrary.setOnClickListener { openSkinLibrary() }
        btnLogout.setOnClickListener { logout() }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        selectImageLauncher.launch(intent)
    }

    private fun loadSkinPreview(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .into(skinImage)
    }

    private fun loadSkin3D(uri: Uri) {
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap?.let {
                skinView3D.setSkinBitmap(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun uploadSkin() {
        selectedSkinUri?.let {
            // TODO: ここにサーバーアップロード処理を追加
            // 例: HTTP POST で送信
        } ?: run {
            // 選択されていない場合
            txtUsername.text = "スキンが選択されていません"
        }
    }

    private fun openSkinLibrary() {
        // TODO: ライブラリ画面に遷移
        txtUsername.text = "スキンライブラリは未実装"
    }

    private fun logout() {
        // TODO: ログアウト処理
        txtUsername.text = "ログアウトしました"
    }
}