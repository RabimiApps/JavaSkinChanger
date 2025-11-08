package com.rabimi.javaskinchanger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent

class WelcomeActivity : AppCompatActivity() {

    private val clientId = "00000000402b5328" // Minecraft用既知ClientID
    private val redirectUri = "https://login.live.com/oauth20_desktop.srf"
    private val scope = "service::user.auth.xboxlive.com::MBI_SSL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val btnNext = findViewById<Button>(R.id.btnNext)
        btnNext.setOnClickListener {
            openMicrosoftLogin()
        }
    }

    private fun openMicrosoftLogin() {
        val loginUrl = "https://login.live.com/oauth20_authorize.srf" +
                "?client_id=$clientId" +
                "&response_type=token" +
                "&redirect_uri=$redirectUri" +
                "&scope=$scope"

        val builder = CustomTabsIntent.Builder()
        builder.setShowTitle(true) // タイトルバーを表示
        builder.setToolbarColor(getColor(R.color.blue)) // ボタン色に合わせる

        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(loginUrl))
    }
}