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

    private val clientId = "00000000402b5328"
    private val redirectUri = "ms-xal-00000000402b5328://auth"
    private val scope = "XboxLive.signin offline_access"

    private lateinit var btnNext: Button
    private val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        btnNext = findViewById(R.id.btnNext)

        // 🔹 既にログイン済みならスキップ
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val savedToken = prefs.getString("minecraft_token", null)
        if (savedToken != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

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
                if (token != null) {
                    fetchMinecraftTokenAndUsername(token)
                } else {
                    showErrorDialog("トークン取得失敗")
                }
            }
        }
    }

    private fun fetchMinecraftTokenAndUsername(msToken: String) {
        mainScope.launch {
            val result = withContext(Dispatchers.IO) { getMinecraftAuth(msToken) }
            if (result != null) {
                val (mcToken, username) = result

                // 🔹 SharedPreferences に保存
                getSharedPreferences("prefs", MODE_PRIVATE)
                    .edit()
                    .putString("minecraft_token", mcToken)
                    .apply()

                showConfirmDialog(username)
            } else {
                showErrorDialog("Minecraft API 取得失敗")
            }
        }
    }

    private fun getMinecraftAuth(msToken: String): Pair<String, String>? {
        return try {
            val xblResp = postJson(
                URL("https://user.auth.xboxlive.com/user/authenticate"),
                """
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
            ) ?: return null

            val xblToken = xblResp.getString("Token")
            val userHash = xblResp.getJSONObject("DisplayClaims")
                .getJSONArray("xui").getJSONObject(0).getString("uhs")

            val xstsResp = postJson(
                URL("https://xsts.auth.xboxlive.com/xsts/authorize"),
                """
                {
                    "Properties": {
                        "SandboxId": "RETAIL",
                        "UserTokens": ["$xblToken"]
                    },
                    "RelyingParty": "rp://api.minecraftservices.com/",
                    "TokenType": "JWT"
                }
                """.trimIndent()
            ) ?: return null

            val xstsToken = xstsResp.getString("Token")

            val mcAuthResp = postJson(
                URL("https://api.minecraftservices.com/authentication/login_with_xbox"),
                """{"identityToken":"XBL3.0 x=$userHash;$xstsToken"}"""
            ) ?: return null

            val mcToken = mcAuthResp.getString("access_token")

            val mcProfileResp = getJson(
                URL("https://api.minecraftservices.com/minecraft/profile"),
                mcToken
            ) ?: return null

            val username = mcProfileResp.getString("name")
            mcToken to username
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun postJson(url: URL, body: String): JSONObject? {
        return try {
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.outputStream.use { it.write(body.toByteArray()) }

            val resp = conn.inputStream.bufferedReader().readText()
            if (conn.responseCode in 200..299 && resp.isNotEmpty()) JSONObject(resp) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getJson(url: URL, token: String): JSONObject? {
        return try {
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val resp = conn.inputStream.bufferedReader().readText()
            if (conn.responseCode in 200..299 && resp.isNotEmpty()) JSONObject(resp) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun showConfirmDialog(username: String) {
        AlertDialog.Builder(this)
            .setTitle("ログイン確認")
            .setMessage("ログイン: $username\nこのアカウントでログインしますか？")
            .setPositiveButton("はい") { _, _ ->
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .setNegativeButton("いいえ", null)
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
