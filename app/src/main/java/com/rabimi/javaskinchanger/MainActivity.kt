package com.rabimi.javaskinchanger

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {

    private lateinit var skinImage: ImageView
    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var btnSwitchModel: Button
    private var selectedUri: Uri? = null
    private var isSteve = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        skinImage = findViewById(R.id.skinImage)
        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        btnSwitchModel = findViewById(R.id.btnSwitchModel)

        btnSelect.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/png"
            startActivityForResult(intent, 1)
        }

        btnUpload.setOnClickListener {
            selectedUri?.let {
                SkinManager.uploadSkin(this, it, isSteve)
            }
        }

        btnSwitchModel.setOnClickListener {
            isSteve = !isSteve
            btnSwitchModel.text = if (isSteve) "スティーブ → アレックス" else "アレックス → スティーブ"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            selectedUri = data?.data
            Glide.with(this).load(selectedUri).into(skinImage)
        }
    }
}