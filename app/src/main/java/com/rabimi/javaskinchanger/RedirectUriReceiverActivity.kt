package com.rabimi.javaskinchanger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

class RedirectUriReceiverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: Uri? = intent?.data
        if (data != null && data.toString().startsWith("javaskinchanger://auth")) {
            val token = data.fragment?.split("&")
                ?.firstOrNull { it.startsWith("access_token=") }
                ?.split("=")
                ?.getOrNull(1)

            if (token != null) {
                // トークン保存
                val sp = getSharedPreferences("prefs", MODE_PRIVATE)
                sp.edit().putString("microsoft_token", token).apply()

                // MainActivity に遷移
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "ログインに失敗しました", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            finish()
        }
    }
}