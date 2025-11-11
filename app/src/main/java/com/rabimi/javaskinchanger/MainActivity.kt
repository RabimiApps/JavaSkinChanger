package com.rabimi.javaskinchanger

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import dev.storeforminecraft.skinviewandroid.library.threedimension.ui.SkinView3DSurfaceView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var skinView: SkinView3DSurfaceView
    private lateinit var skinImage: ImageView
    private lateinit var txtUsername: TextView
    private lateinit var btnSelect: Button

    private val mainScope = MainScope()
    private var selectedUri: Uri? = null

    // ActivityResult for picking image
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            val bitmap = contentResolver.openInputStream(it)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
            if (bitmap != null) {
                skinImage.setImageBitmap(bitmap)        // 2D プレビュー
                setSkinToView(bitmap)                    // 3D SkinView に描画
            } else {
                Toast.makeText(this, "画像の読み込みに失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        skinView = findViewById(R.id.skinView)
        skinImage = findViewById(R.id.skinImage)
        txtUsername = findViewById(R.id.txtUsername)
        btnSelect = findViewById(R.id.btnSelect)

        btnSelect.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }
    }

    // --- SkinView3DSurfaceView にスキンを描画する自作関数 ---
    private fun setSkinToView(bitmap: Bitmap) {
        // SkinView3DSurfaceView の Surface がまだ初期化されていない場合があるので
        // post で描画キューに入れると安全
        skinView.post {
            try {
                // ここで SkinView3DSurfaceView の描画メソッドを呼ぶ
                // もし setBitmap 等が AAR にあればそれを使う
                skinView.setBitmap(bitmap) // 仮にメソッドがある場合
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "スキンの表示に失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        mainScope.cancel()
        super.onDestroy()
    }
}