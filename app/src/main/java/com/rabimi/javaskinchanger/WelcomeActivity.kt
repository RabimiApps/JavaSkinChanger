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

    // Minecraft公式用既知ClientID（PojavLauncherと同じ）
    private val clientId = "00000000402b5328"
    // 公式ClientIDの場合はMicrosoftが決めたリダイレクトURIを使用
    private val redirectUri = "https://login.live.com/oauth20_desktop.srf"
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

            // Microsoft OAuth2 認証ページ URL
            val loginUrl = "https://login.live.com/oauth20_authorize.srf" +
                    "?client_id=$clientId" +
                    "&response_type=token" +
                    "&redirect_uri=$redirectUri" +
                    "&scope=$scope"

            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    // redirectUri に遷移したらトークンを取得
                    if (url.startsWith(redirectUri)) {
                        if (url.contains("#access_token=")) {
                            val token = Uri.parse(url.replace("#", "?"))
                                .getQueryParameter("access_token")
                            if (token != null) {
                                saveTokenAndContinue(token)
                            } else {
                                // トークンが無い場合は失敗としてToast表示
                                android.widget.Toast.makeText(
                                    this@WelcomeActivity,
                                    "ログイン失敗",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
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

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}