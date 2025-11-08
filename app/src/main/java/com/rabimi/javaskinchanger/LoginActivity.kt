package com.rabimi.javaskinchanger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val redirectUri = "https://login.live.com/oauth20_desktop.srf" // Microsoft公式推奨

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        webView = findViewById(R.id.webViewLogin)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    // リダイレクトURLにアクセストークンが返ってくる
                    if (it.startsWith(redirectUri)) {
                        val uri = Uri.parse(it)
                        val accessToken = uri.getQueryParameter("access_token")
                        if (accessToken != null) {
                            Toast.makeText(this@LoginActivity, "ログイン成功", Toast.LENGTH_SHORT).show()
                            // MainActivityに戻す
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.putExtra("access_token", accessToken)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this@LoginActivity, "ログイン失敗", Toast.LENGTH_SHORT).show()
                        }
                        return true
                    }
                }
                return false
            }
        }

        // Microsoft OAuth2 認証ページを開く
        val clientId = "YOUR_CLIENT_ID" // Microsoft アプリ登録で取得
        val scope = "XboxLive.signin offline_access"
        val url = "https://login.live.com/oauth20_authorize.srf?client_id=$clientId&response_type=token&redirect_uri=$redirectUri&scope=$scope"
        webView.loadUrl(url)
    }
}