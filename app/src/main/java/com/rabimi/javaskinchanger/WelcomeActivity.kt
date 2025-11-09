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

    // Java Edition 用 ClientId と Redirect URI
    private val clientId = "00000000402b5328"
    private val redirectUri = "ms-xal-00000000402b5328://auth"
    private val scope = "XboxLive.signin offline_access"

    private lateinit var btnNext: Button
    private val mainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        btnNext = findViewById(R.id.btnNext)
        println("[Debug] onCreate called")

        btnNext.setOnClickListener {
            println("[Debug] btnNext clicked")
            val loginUrl = "https://login.live.com/oauth20_authorize.srf" +
                    "?client_id=$clientId" +
                    "&response_type=token" +
                    "&redirect_uri=$redirectUri" +
                    "&scope=$scope" +
                    "&prompt=select_account"
            println("[Debug] Launching OAuth URL: $loginUrl")
            CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(loginUrl))
        }

        handleRedirect(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        println("[Debug] onNewIntent called")
        handleRedirect(intent)
    }

    private fun handleRedirect(intent: Intent?) {
        intent?.data?.let { uri ->
            println("[Debug] handleRedirect with URI: $uri")
            if (uri.toString().startsWith(redirectUri)) {
                val fragment = uri.fragment ?: ""
                println("[Debug] URI Fragment: $fragment")
                val token = fragment.split("&").find { it.startsWith("access_token=") }
                    ?.substringAfter("=")
                if (token != null) {
                    println("[Debug] Access Token: $token")
                    fetchMinecraftUsername(token)
                } else {
                    println("[Debug] Access token not found in fragment")
                    showErrorDialog("トークン取得失敗")
                }
            }
        }
    }

    private fun fetchMinecraftUsername(msToken: String) {
        println("[Debug] fetchMinecraftUsername called with msToken: $msToken")
        mainScope.launch {
            val username = withContext(Dispatchers.IO) { getMinecraftUsername(msToken) }
            if (username != null) {
                println("[Debug] Minecraft Username: $username")
                showConfirmDialog(username)
            } else {
                println("[Debug] Minecraft API取得失敗")
                showErrorDialog("Minecraft API 取得失敗")
            }
        }
    }

    private fun getMinecraftUsername(msToken: String): String? {
        return try {
            // Xbox Live 認証
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

            // XSTS 認証
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

            // Minecraft API 認証
            val mcAuthResp = postJson(
                URL("https://api.minecraftservices.com/authentication/login_with_xbox"),
                """{"identityToken":"XBL3.0 x=$userHash;$xstsToken"}"""
            ) ?: return null

            val mcToken = mcAuthResp.getString("access_token")

            // Minecraft プロフィール取得
            val mcProfileResp = getJson(
                URL("https://api.minecraftservices.com/minecraft/profile"),
                mcToken
            ) ?: return null

            mcProfileResp.getString("name")
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

            val resp = try {
                conn.inputStream.bufferedReader().readText()
            } catch (ee: Exception) {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }

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

            val resp = try {
                conn.inputStream.bufferedReader().readText()
            } catch (ee: Exception) {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }

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