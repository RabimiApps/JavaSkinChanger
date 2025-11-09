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
import java.net.HttpURLConnection
import java.net.URL

class WelcomeActivity : AppCompatActivity() {

    private val clientId = "0000000048183522"
    private val redirectUri = "ms-xal-0000000048183522://auth"
    private val scope = "XboxLive.signin offline_access" // 最新スコープ

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

            CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(loginUrl))
        }

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
                    ?.substringAfter("=")
                println("MS Token: $token") // デバッグ用
                if (token != null) {
                    fetchMinecraftUsername(token)
                } else {
                    showErrorDialog("トークン取得失敗")
                }
            }
        }
    }

    private fun fetchMinecraftUsername(msToken: String) {
        mainScope.launch {
            val username = withContext(Dispatchers.IO) { getMinecraftUsername(msToken) }
            if (username != null) {
                showConfirmDialog(username)
            } else {
                showErrorDialog("Minecraft API 取得失敗")
            }
        }
    }

    private fun getMinecraftUsername(msToken: String): String? {
        try {
            // 1️⃣ Xbox Live Authentication
            val xblResp = postJson(
                URL("https://user.auth.xboxlive.com/user/authenticate"), """
                {
                    "Properties": {
                        "AuthMethod": "RPS",
                        "SiteName": "user.auth.xboxlive.com",
                        "RpsTicket": "d=$msToken"
                    },
                    "RelyingParty": "http://auth.xboxlive.com",
                    "TokenType": "JWT"
                }
            """.trimIndent()
            )
            if (xblResp == null) {
                println("❌ Xbox Live Authentication 失敗")
                return null
            }

            val xblToken = xblResp.getString("Token")
            val userHash = xblResp.getJSONObject("DisplayClaims")
                .getJSONArray("xui").getJSONObject(0).getString("uhs")
            println("✅ Xbox Live OK")
            println("XBL Token: $xblToken")
            println("User Hash: $userHash")

            // 2️⃣ XSTS Token
            val xstsResp = postJson(
                URL("https://xsts.auth.xboxlive.com/xsts/authorize"), """
                {
                    "Properties": {
                        "SandboxId": "RETAIL",
                        "UserTokens": ["$xblToken"]
                    },
                    "RelyingParty": "rp://api.minecraftservices.com",
                    "TokenType": "JWT"
                }
            """.trimIndent()
            )
            if (xstsResp == null) {
                println("❌ XSTS Token 取得失敗")
                return null
            }

            val xstsToken = xstsResp.getString("Token")
            println("✅ XSTS Token OK")
            println("XSTS Token: $xstsToken")

            // 3️⃣ Minecraft Access Token
            val mcAuthUrl = URL("https://api.minecraftservices.com/authentication/login_with_xbox")
            val mcAuthBody = """{"identityToken":"XBL3.0 x=$userHash;$xstsToken"}"""
            val mcConn = mcAuthUrl.openConnection() as HttpURLConnection
            mcConn.requestMethod = "POST"
            mcConn.doOutput = true
            mcConn.setRequestProperty("Content-Type", "application/json")
            mcConn.outputStream.use { it.write(mcAuthBody.toByteArray()) }

            val mcToken = if (mcConn.responseCode == 200) {
                JSONObject(mcConn.inputStream.bufferedReader().readText()).getString("access_token")
            } else {
                println("❌ Minecraft Access Token 取得失敗: ${mcConn.responseCode}")
                return null
            }
            println("✅ Minecraft Access Token OK")
            println("Minecraft Token: $mcToken")

            // 4️⃣ Minecraft Profile
            val mcUrl = URL("https://api.minecraftservices.com/minecraft/profile")
            val conn = mcUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $mcToken")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            return if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val username = JSONObject(response).getString("name")
                println("✅ Minecraft Profile OK: $username")
                username
            } else {
                println("❌ Minecraft Profile 取得失敗: ${conn.responseCode}")
                null
            }

        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Exception 発生: ${e.message}")
            return null
        }
    }

    private fun postJson(url: URL, body: String): JSONObject? {
        try {
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toByteArray()) }

            return if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader().readText()
                JSONObject(resp)
            } else {
                println("❌ POST リクエスト失敗: ${conn.responseCode} -> $url")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ POST Exception: ${e.message} -> $url")
            return null
        }
    }

    private fun showConfirmDialog(username: String) {
        AlertDialog.Builder(this)
            .setTitle("ログイン確認")
            .setMessage("ログイン: $username\nこのアカウントでログインしますか？")
            .setPositiveButton("はい") { _, _ ->
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("minecraft_username", username)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("いいえ") { d, _ -> d.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(msg: String) {
        AlertDialog.Builder(this)
            .setTitle("エラー")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        mainScope.cancel()
        super.onDestroy()
    }
}