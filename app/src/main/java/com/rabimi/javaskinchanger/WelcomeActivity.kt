package com.rabimi.javaskinchanger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class WelcomeActivity : AppCompatActivity() {

    // 公式 Minecraft ClientId
    private val clientId = "0000000048183522"
    // 公式リダイレクト URI
    private val redirectUri = "ms-xal-0000000048183522://auth"
    private val scope = "service::user.auth.xboxlive.com::MBI_SSL"

    private lateinit var btnNext: Button
    private val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        btnNext = findViewById(R.id.btnNext)

        btnNext.setOnClickListener {
            val loginUrl = "https://login.live.com/oauth20_authorize.srf" +
                    "?client_id=$clientId" +
                    "&response_type=token" +
                    "&redirect_uri=$redirectUri" +
                    "&scope=$scope" +
                    "&prompt=select_account"

            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(this, Uri.parse(loginUrl))
        }

        // アプリに戻ってきた場合の処理
        handleRedirect(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleRedirect(intent)
    }

    private fun handleRedirect(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.toString().startsWith(redirectUri)) {
                val fragment = uri.fragment ?: ""
                val token = fragment.split("&").find { it.startsWith("access_token=") }
                    ?.split("=")?.getOrNull(1)

                if (token != null) {
                    fetchMinecraftUsername(token)
                } else {
                    showErrorDialog("トークン取得失敗")
                }
            }
        }
    }

    private fun fetchMinecraftUsername(accessToken: String) {
        mainScope.launch {
            val username = withContext(Dispatchers.IO) { getMinecraftUsername(accessToken) }
            if (username != null) {
                showConfirmDialog(username)
            } else {
                showErrorDialog("Minecraft API 取得失敗")
            }
        }
    }

    private fun showConfirmDialog(username: String) {
        AlertDialog.Builder(this)
            .setTitle("ログイン確認")
            .setMessage("ログイン: $username\nこのアカウントでログインしますか？")
            .setPositiveButton("はい") { _, _ ->
                val intent = Intent(this@WelcomeActivity, MainActivity::class.java)
                intent.putExtra("minecraft_username", username)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("いいえ") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("エラー")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun getMinecraftUsername(accessToken: String): String? {
        return try {
            // Xbox Live 認証
            val xboxResponse = xboxLogin(accessToken)
            val mcAccessToken = xboxResponse?.getString("Token") ?: return null

            // Minecraft profile API
            val mcUrl = URL("https://api.minecraftservices.com/minecraft/profile")
            val conn = mcUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $mcAccessToken")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                JSONObject(response).getString("name")
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun xboxLogin(accessToken: String): JSONObject? {
        return try {
            val url = URL("https://user.auth.xboxlive.com/user/authenticate")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            val body = """
                {
                    "Properties": {
                        "AuthMethod": "RPS",
                        "SiteName": "user.auth.xboxlive.com",
                        "RpsTicket": "d=$accessToken"
                    },
                    "RelyingParty": "http://auth.xboxlive.com",
                    "TokenType": "JWT"
                }
            """.trimIndent()
            val out: OutputStream = conn.outputStream
            out.write(body.toByteArray())
            out.flush()
            out.close()

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                JSONObject(response)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onDestroy() {
        mainScope.cancel()
        super.onDestroy()
    }
}