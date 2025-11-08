package com.rabimi.javaskinchanger

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import net.openid.appauth.*
import android.net.Uri

class WelcomeActivity : AppCompatActivity() {

    private lateinit var authService: AuthorizationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        authService = AuthorizationService(this)

        val btnNext = findViewById<Button>(R.id.btnNext)
        btnNext.setOnClickListener {
            startMicrosoftLogin()
        }
    }

    private fun startMicrosoftLogin() {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse("https://login.microsoftonline.com/common/oauth2/v2.0/authorize"), // Auth endpoint
            Uri.parse("https://login.microsoftonline.com/common/oauth2/v2.0/token")      // Token endpoint
        )

        val clientId = "YOUR_MICROSOFT_APP_CLIENT_ID"
        val redirectUri = Uri.parse("javaskinchanger://oauth2redirect")
        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        ).setScopes("XboxLive.signin offline_access").build()

        val intent = authService.getAuthorizationRequestIntent(authRequest)
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            val resp = AuthorizationResponse.fromIntent(data!!)
            val ex = AuthorizationException.fromIntent(data)

            if (resp != null) {
                val tokenRequest = resp.createTokenExchangeRequest()
                authService.performTokenRequest(tokenRequest) { response, exception ->
                    if (response != null) {
                        val accessToken = response.accessToken!!
                        // 保存
                        val sp = getSharedPreferences("prefs", MODE_PRIVATE)
                        sp.edit().putString("microsoft_token", accessToken).apply()

                        // メイン画面へ
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}