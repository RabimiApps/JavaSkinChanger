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
            } else {
                println("[Debug] URI does not match redirectUri")
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
            println("[Debug] Starting Xbox Live Authentication")
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
            ) ?: run {
                println("[Debug] Xbox Live Authentication failed")
                return null
            }

            val xblToken = xblResp.getString("Token")
            val userHash = xblResp.getJSONObject("DisplayClaims")
                .getJSONArray("xui").getJSONObject(0).getString("uhs")

            println("[Debug] XBL Token: $xblToken")
            println("[Debug] User Hash: $userHash")

            println("[Debug] Starting XSTS Authentication")
            val xstsResp = postJson(
                URL("https://xsts.auth.xboxlive.com/xsts/authorize"),
                """
                {
                    "Properties": {
                        "SandboxId": "RETAIL",
                        "UserTokens": ["$xblToken"]
                    },
                    "RelyingParty": "rp://api.minecraftservices.com",
                    "TokenType": "JWT"
                }
                """.trimIndent()
            ) ?: run {
                println("[Debug] XSTS Authentication failed")
                return null
            }

            val xstsToken = xstsResp.getString("Token")
            println("[Debug] XSTS Token: $xstsToken")

            println("[Debug] Starting Minecraft Authentication")
            val mcAuthUrl = URL("https://api.minecraftservices.com/authentication/login_with_xbox")
            val mcAuthBody = """{"identityToken":"XBL3.0 x=$userHash;$xstsToken"}"""
            val mcConn = mcAuthUrl.openConnection() as HttpURLConnection
            mcConn.requestMethod = "POST"
            mcConn.doOutput = true
            mcConn.setRequestProperty("Content-Type", "application/json")
            mcConn.outputStream.use { it.write(mcAuthBody.toByteArray()) }

            val mcToken = if (mcConn.responseCode == 200) {
                val resp = JSONObject(mcConn.inputStream.bufferedReader().readText())
                resp.getString("access_token").also { println("[Debug] Minecraft Token: $it") }
            } else {
                println("[Debug] Minecraft Authentication failed with code ${mcConn.responseCode}")
                return null
            }

            println("[Debug] Fetching Minecraft Profile")
            val mcUrl = URL("https://api.minecraftservices.com/minecraft/profile")
            val conn = mcUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $mcToken")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            return if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val username = JSONObject(response).getString("name")
                println("[Debug] Minecraft Profile Response: $response")
                username
            } else {
                println("[Debug] Minecraft Profile fetch failed with code ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("[Debug] Exception in getMinecraftUsername: ${e.message}")
            null
        }
    }

    private fun postJson(url: URL, body: String): JSONObject? {
        return try {
            println("[Debug] POST JSON to $url")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toByteArray()) }

            return if (conn.responseCode == 200) {
                val resp = conn.inputStream.bufferedReader().readText()
                println("[Debug] Response code: ${conn.responseCode}")
                println("[Debug] Response body: $resp")
                JSONObject(resp)
            } else {
                println("[Debug] Request failed with code ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("[Debug] Exception in postJson: ${e.message}")
            null
        }
    }

    private fun showConfirmDialog(username: String) {
        println("[Debug] showConfirmDialog called with username: $username")
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
        println("[Debug] showErrorDialog: $msg")
        AlertDialog.Builder(this)
            .setTitle("エラー")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        println("[Debug] onDestroy called")
        mainScope.cancel()
        super.onDestroy()
    }
}