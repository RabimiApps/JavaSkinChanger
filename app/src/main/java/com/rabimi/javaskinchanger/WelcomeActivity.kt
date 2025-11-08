package com.rabimi.javaskinchanger

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    private val clientId = "00000000402b5328"
    private val redirectUri = "https://login.live.com/oauth20_desktop.srf"
    private val scope = "service::user.auth.xboxlive.com::MBI_SSL"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val btnNext = findViewById<Button>(R.id.btnNext)
        btnNext.setOnClickListener {
            startMicrosoftLogin()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun startMicrosoftLogin() {
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true // ← これないとログイン画面が真っ白になることがある
        setContentView(webView)

        val loginUrl = Uri.Builder()
            .scheme("https")
            .authority("login.live.com")
            .path("oauth20_authorize.srf")
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "token")
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("scope", scope)
            .build()
            .toString()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url.startsWith(redirectUri) && url.contains("#access_token=")) {
                    val token = Uri.parse(url.replace("#", "?")).getQueryParameter("access_token")
                    if (token != null) {
                        saveTokenAndContinue(token)
                        return true
                    }
                }
                return false
            }

            // 古いAndroid互換用
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null && url.startsWith(redirectUri) && url.contains("#access_token=")) {
                    val token = Uri.parse(url.replace("#", "?")).getQueryParameter("access_token")
                    if (token != null) {
                        saveTokenAndContinue(token)
                        return true
                    }
                }
                return false
            }
        }

        webView.loadUrl(loginUrl)
    }

    private fun saveTokenAndContinue(token: String) {
        val sp = getSharedPreferences("prefs", MODE_PRIVATE)
        sp.edit().putString("microsoft_token", token).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}