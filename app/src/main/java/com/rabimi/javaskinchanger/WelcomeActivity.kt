package com.rabimi.javaskinchanger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent

class WelcomeActivity : AppCompatActivity() {

    private val clientId = "00000000402b5328" // Minecraft公式ClientID
    private val redirectUri = "javaskinchanger://auth"
    private val scope = "XboxLive.signin offline_access"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val btnNext = findViewById<Button>(R.id.btnNext)
        btnNext.setOnClickListener { openMicrosoftLogin() }
    }

    private fun openMicrosoftLogin() {
        val url = "https://login.live.com/oauth20_authorize.srf" +
                "?client_id=$clientId" +
                "&response_type=token" +
                "&redirect_uri=$redirectUri" +
                "&scope=$scope"

        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        customTabsIntent.launchUrl(this, Uri.parse(url))
    }
}