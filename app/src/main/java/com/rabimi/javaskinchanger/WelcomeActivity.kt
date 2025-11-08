package com.rabimi.javaskinchanger

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    private val redirectUri = "https://login.live.com/oauth20_desktop.srf" // Minecraft公式リダイレクト
    private val scope = "service::user.auth.xboxlive.com::MBI_SSL"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val btnNext = findViewById<Button>(R.id.btnNext)
        btnNext.setOnClickListener {
            val webView = WebView(this)
            webView.settings.javaScriptEnabled = true
            setContentView(webView)

            // 公式 Microsoft ログイン URL（Pojav方式）
            val loginUrl = "https://login.live.com/oauth20_authorize.srf" +
                    "?client_id=00000000402b5328" +   // Minecraft 用既知 ClientID
                    "&response_type=token" +
                    "&redirect_uri=$redirectUri" +
                    "&scope=$scope"

            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    if (url.startsWith(redirectUri)) {
                        // URL に #access_token=XXXX が含まれる
                        if (url.contains("#access_token=")) {
                            val token = Uri.parse(url.replace("#", "?"))
                                .getQueryParameter("access_token")
                            if (token != null) {
                                saveTokenAndContinue(token)
                            }
                        }
                        return true
                    }
                    return false
                }
            }

            webView.loadUrl(loginUrl)
        }
    }

    private fun saveTokenAndContinue(token: String) {
        val sp = getSharedPreferences("prefs", MODE_PRIVATE)
        sp.edit().putString("microsoft_token", token).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}